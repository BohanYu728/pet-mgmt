package com.petmgmt.ai;

import ai.onnxruntime.*;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.FloatBuffer;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.List;

/** Offline ImageNet inference; species is never inferred from the filename or pet profile. */
public final class PetImageClassifier {
    private static final Map<String,String> NAMES=Map.ofEntries(
        Map.entry("Border collie","边境牧羊犬"),Map.entry("collie","柯利牧羊犬"),
        Map.entry("golden retriever","金毛寻回犬"),Map.entry("Labrador retriever","拉布拉多寻回犬"),
        Map.entry("Siberian husky","西伯利亚哈士奇"),Map.entry("Pembroke","彭布罗克柯基犬"),
        Map.entry("Samoyed","萨摩耶犬"),Map.entry("German shepherd","德国牧羊犬"),
        Map.entry("tabby","虎斑猫"),Map.entry("tiger cat","虎斑猫"),Map.entry("Persian cat","波斯猫"),
        Map.entry("Siamese cat","暹罗猫"),Map.entry("Egyptian cat","埃及猫"));
    private static class Model {
        final OrtEnvironment env=OrtEnvironment.getEnvironment();
        final OrtSession session;
        final List<String> labels;
        Model() {
            try(var data=PetImageClassifier.class.getResourceAsStream("/models/mobilenetv2-7.onnx");
                var names=PetImageClassifier.class.getResourceAsStream("/models/imagenet_classes.txt");
                var options=new OrtSession.SessionOptions()) {
                if(data==null||names==null)throw new IOException("Missing model resources");
                options.setIntraOpNumThreads(2);
                session=env.createSession(data.readAllBytes(),options);
                labels=new String(names.readAllBytes(),StandardCharsets.UTF_8).lines().toList();
                if(labels.size()!=1000)throw new IOException("Invalid class list");
            }catch(IOException|OrtException ex){throw new IllegalStateException("图片分类模型加载失败",ex);}
        }
    }
    private static volatile Model model;
    private static synchronized Model model(){if(model==null)model=new Model();return model;}
    public static BufferedImage readImage(String path) {
        try {
            BufferedImage image=ImageIO.read(new File(path));
            if(image==null)throw new IOException("Invalid image");
            if(image.getWidth()<64||image.getHeight()<64||image.getWidth()>4096||image.getHeight()>4096)
                throw new IllegalArgumentException("图片宽高须在 64 至 4096 像素之间");
            return image;
        }catch(IOException ex){throw new IllegalArgumentException("无法读取图片，请上传有效的 JPG 或 PNG 照片",ex);}
    }
    public Map<String,Object> predict(String path) {
        BufferedImage source=readImage(path);
        // Resize the short edge to 256, center crop 224, RGB ImageNet normalization.
        double scale=256.0/Math.min(source.getWidth(),source.getHeight());
        int w=(int)Math.round(source.getWidth()*scale),h=(int)Math.round(source.getHeight()*scale);
        BufferedImage crop=new BufferedImage(224,224,BufferedImage.TYPE_INT_RGB);
        Graphics2D g=crop.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(source,(224-w)/2,(224-h)/2,w,h,null);g.dispose();
        float[] input=new float[3*224*224],mean={.485f,.456f,.406f},std={.229f,.224f,.225f};
        for(int y=0;y<224;y++)for(int x=0;x<224;x++) {
            int rgb=crop.getRGB(x,y);
            for(int c=0;c<3;c++)input[c*224*224+y*224+x]=(((rgb>>(16-8*c))&255)/255f-mean[c])/std[c];
        }
        Model m=model();
        try(OnnxTensor tensor=OnnxTensor.createTensor(m.env,FloatBuffer.wrap(input),new long[]{1,3,224,224});
            OrtSession.Result prediction=m.session.run(Map.of(m.session.getInputNames().iterator().next(),tensor))) {
            float[] logits=((float[][])prediction.get(0).getValue())[0];
            double max=-Double.MAX_VALUE;for(float v:logits)max=Math.max(max,v);
            double[] p=new double[1000];double sum=0;
            for(int i=0;i<1000;i++)sum+=(p[i]=Math.exp(logits[i]-max));
            for(int i=0;i<1000;i++)p[i]/=sum;
            int top=0;double dog=0,cat=0;
            for(int i=0;i<1000;i++){if(p[i]>p[top])top=i;if(i>=151&&i<=268)dog+=p[i];if(i>=281&&i<=285)cat+=p[i];}
            String species=dog>=.6&&top>=151&&top<=268?"dog":cat>=.6&&top>=281&&top<=285?"cat":"unknown";
            boolean known=!species.equals("unknown");String label=m.labels.get(top);
            String name=known?NAMES.getOrDefault(label,label)+(species.equals("dog")?"（犬类候选）":"（猫类候选）"):"无法确认猫狗种类";
            Map<String,Object> result=new LinkedHashMap<>();
            result.put("species",species);result.put("analysis_mode","mobilenetv2_imagenet");
            result.put("name_zh",name);result.put("breed_name",name);
            result.put("confidence",known?Math.round(p[top]*10000)/100.0:null);
            result.put("species_confidence",known?Math.round((species.equals("dog")?dog:cat)*10000)/100.0:null);
            result.put("description",known?"图片模型判断为"+(species.equals("dog")?"狗狗":"猫咪")+"。候选品种与模型分数仅供参考，不是准确率；混种、遮挡和背景可能影响结果。":"模型未获得可靠的猫狗分类结果，请使用主体清晰的单只宠物照片重试。其他动物可能不在支持范围内。");
            result.put("care_tips","请根据实际宠物档案安排照护，不应依据照片候选品种决定健康处理。");
            return result;
        }catch(OrtException ex){throw new IllegalStateException("图片分类推理失败，请稍后重试",ex);}
    }
}
