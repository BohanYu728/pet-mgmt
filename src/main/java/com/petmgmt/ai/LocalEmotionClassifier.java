package com.petmgmt.ai;

import ai.onnxruntime.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.FloatBuffer;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.List;

/** CLIP similarity to fixed cat/dog descriptions; not a calibrated emotion diagnosis. */
public final class LocalEmotionClassifier {
    private static final String[] KEYS={"happy","relaxed","alert","anxious","angry","sleepy"};
    private static final String[] ZH={"开心或活跃","放松","警觉或好奇","紧张或害怕","生气或防御","困倦"};
    private static final String[][] WORDS={{"happy","playful","excited"},{"relaxed","calm","comfortable"},
        {"alert","attentive","curious"},{"anxious","fearful","nervous"},{"angry","aggressive","defensive"},{"sleepy","sleeping","drowsy"}};
    private static class Model {
        final OrtEnvironment env=OrtEnvironment.getEnvironment();
        final OrtSession vision;
        final Map<String,float[][]> descriptions=new HashMap<>();

        Model() {
            try(var options=new OrtSession.SessionOptions()) {
                options.setIntraOpNumThreads(2);
                vision=env.createSession(resource("vision.onnx"),options);
                Map<String,Integer> vocab=new ObjectMapper().readValue(resource("vocab.json"),new TypeReference<>(){});
                Map<String,Integer> ranks=new HashMap<>();
                for(String merge:new String(resource("merges.txt"),StandardCharsets.UTF_8).lines().toList())
                    if(!merge.startsWith("#")&&!merge.isBlank())ranks.put(merge,ranks.size());
                try(OrtSession text=env.createSession(resource("text.onnx"),options)) {
                    for(String species:List.of("cat","dog","subject")) {
                        float[][] vectors=new float[6][512];
                        for(int c=0;c<6;c++) {
                            for(String adjective:WORDS[c]) {
                                String prompt=species.equals("subject") ? new String[]{"a photo of a cat", "a photo of a dog", "a photo of a person without any pets", "a photo of a landscape", "a photo of a household object", "a photo of a bird"}[c] : "a photo of a "+adjective+" "+species;
                                long[][] ids={tokenize(prompt,vocab,ranks)},mask={new long[77]};
                                boolean ended=false;
                                for(int i=0;i<77;i++){if(!ended)mask[0][i]=1;if(ids[0][i]==49407)ended=true;}
                                try(var tokens=OnnxTensor.createTensor(env,ids);var attention=OnnxTensor.createTensor(env,mask)) {
                                    Map<String,OnnxTensor> inputs=new HashMap<>();inputs.put("input_ids",tokens);
                                    if(text.getInputNames().contains("attention_mask"))inputs.put("attention_mask",attention);
                                    try(var result=text.run(inputs)) {
                                        float[] vector=((float[][])result.get("text_embeds").orElseThrow().getValue())[0];
                                        normalize(vector);for(int k=0;k<512;k++)vectors[c][k]+=vector[k]/3f;
                                    }
                                }
                            }
                            normalize(vectors[c]);
                        }
                        descriptions.put(species,vectors);
                    }
                }
            }catch(IOException|OrtException ex){throw new IllegalStateException("本地情绪视觉模型加载失败",ex);}
        }
    }
    private static volatile Model instance;
    private static synchronized Model model(){if(instance==null)instance=new Model();return instance;}
    private static byte[] resource(String name)throws IOException {
        try(var in=LocalEmotionClassifier.class.getResourceAsStream("/models/clip/"+name)) {
            if(in==null)throw new IOException("Missing CLIP resource: "+name);return in.readAllBytes();
        }
    }
    // Fixed prompts contain ASCII words only; CLIP byte encoding leaves these letters unchanged.
    static long[] tokenize(String prompt,Map<String,Integer> vocab,Map<String,Integer> ranks) {
        List<Long> ids=new ArrayList<>();ids.add(49406L);
        for(String word:prompt.split(" ")) {
            List<String> parts=new ArrayList<>();for(char ch:word.toCharArray())parts.add(String.valueOf(ch));
            int last=parts.size()-1;parts.set(last,parts.get(last)+"</w>");
            while(parts.size()>1) {
                int pos=-1,rank=Integer.MAX_VALUE;
                for(int i=0;i<parts.size()-1;i++) {
                    int candidate=ranks.getOrDefault(parts.get(i)+" "+parts.get(i+1),Integer.MAX_VALUE);
                    if(candidate<rank){rank=candidate;pos=i;}
                }
                if(pos<0)break;parts.set(pos,parts.get(pos)+parts.remove(pos+1));
            }
            for(String part:parts)ids.add(vocab.get(part).longValue());
        }
        ids.add(49407L);long[] result=new long[77];
        if(ids.size()>77)throw new IllegalArgumentException("CLIP prompt exceeds token limit");
        for(int i=0;i<ids.size();i++)result[i]=ids.get(i);return result;
    }
    private static void normalize(float[] vector) {
        double norm=0;for(float v:vector)norm+=v*v;norm=Math.sqrt(norm);
        if(norm==0||!Double.isFinite(norm))throw new IllegalStateException("Invalid image embedding");
        for(int i=0;i<vector.length;i++)vector[i]/=(float)norm;
    }
    public Map<String,Object> predict(String path) {
        BufferedImage source=PetImageClassifier.readImage(path);
        String species="unknown";
        Map<String,Object> result=new LinkedHashMap<>();
        result.put("species",species);result.put("analysis_mode","local_clip_zero_shot");result.put("confidence",null);
        result.put("comfort_advice","结果是本地视觉模型对图片与状态描述的相似度推测，未经宠物情绪数据集校准，不能证明真实情绪。请观察耳朵、尾巴、姿态、活动与拍摄前后的行为，结合持续记录判断。");
        double scale=224.0/Math.max(source.getWidth(),source.getHeight());
        int w=(int)Math.round(source.getWidth()*scale),h=(int)Math.round(source.getHeight()*scale);
        BufferedImage crop=new BufferedImage(224,224,BufferedImage.TYPE_INT_RGB);Graphics2D g=crop.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.drawImage(source,(224-w)/2,(224-h)/2,w,h,null);g.dispose();
        float[] input=new float[3*224*224],mean={.48145466f,.4578275f,.40821073f},std={.26862954f,.26130258f,.27577711f};
        for(int y=0;y<224;y++)for(int x=0;x<224;x++)for(int c=0;c<3;c++)input[c*224*224+y*224+x]=(((crop.getRGB(x,y)>>(16-8*c))&255)/255f-mean[c])/std[c];
        Model m=model();
        try(var tensor=OnnxTensor.createTensor(m.env,FloatBuffer.wrap(input),new long[]{1,3,224,224});
            var inference=m.vision.run(Map.of("pixel_values",tensor))) {
            float[] vector=((float[][])inference.get("image_embeds").orElseThrow().getValue())[0];normalize(vector);
            double[] subjectScores=new double[6]; int best=0;
            for(int c=0;c<6;c++) {
                for(int k=0;k<512;k++)subjectScores[c]+=vector[k]*m.descriptions.get("subject")[c][k];
                if(subjectScores[c]>subjectScores[best])best=c;
            }
            if(best<=1 && subjectScores[best]>=.2) species=best==0?"cat":"dog";
            result.put("species",species);
            if(species.equals("unknown")) {
                result.put("emotion","uncertain");result.put("emotion_zh","未确认猫狗主体，无法分析");result.put("candidates",List.of());return result;
            }

            List<Map<String,Object>> candidates=new ArrayList<>();double[] scores=new double[6];
            for(int c=0;c<6;c++) {
                for(int k=0;k<512;k++)scores[c]+=vector[k]*m.descriptions.get(species)[c][k];
                candidates.add(Map.of("emotion",KEYS[c],"label",ZH[c],"similarity",Math.round(scores[c]*10000)/10000.0));
            }
            candidates.sort(Comparator.comparingDouble((Map<String,Object> c)->((Number)c.get("similarity")).doubleValue()).reversed());
            var top=candidates.get(0);double gap=((Number)top.get("similarity")).doubleValue()-((Number)candidates.get(1).get("similarity")).doubleValue();
            boolean uncertain=gap<.01||((Number)top.get("similarity")).doubleValue()<.2;
            result.put("emotion",uncertain?"uncertain":top.get("emotion"));
            result.put("emotion_zh",uncertain?"可能"+top.get("label")+"（候选接近，暂无法确定）":"可能"+top.get("label"));
            result.put("similarity",top.get("similarity"));result.put("candidates",candidates);
            result.put("comfort_advice", "主要候选："+top.get("label")+"，图文相似度 "+top.get("similarity")+"。"+result.get("comfort_advice"));
            result.put("uncertainty_note",uncertain?"不同状态的相似度接近，请结合行为观察。":"仅表示图片更接近该状态描述，可能受背景、人物和姿态影响。");
            return result;
        }catch(OrtException ex){throw new IllegalStateException("本地情绪视觉推理失败",ex);}
    }
}
