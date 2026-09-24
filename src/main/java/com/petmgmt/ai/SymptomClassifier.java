package com.petmgmt.ai;

import java.util.*;

/**
 * Naive Bayes classifier for pet symptoms → disease prediction.
 * Chinese text tokenization using character bigrams + medical keywords.
 */
public class SymptomClassifier {

    private static final String[] KEYWORDS = {
        "呕吐","腹泻","拉稀","咳嗽","发烧","精神","食欲","掉毛",
        "瘙痒","红肿","流泪","跛行","便秘","尿血","便血","打喷嚏",
        "流鼻涕","消瘦","贫血","口臭","牙结石","耳螨","跳蚤",
        "不吃","不喝","嗜睡","频繁","排尿","困难","疼痛",
        "皮肤","眼睛","耳朵","口腔","肠胃","关节","呼吸","心脏"
    };

    private Map<String, Integer> vocab = new LinkedHashMap<>();
    private Map<String, Double> classProbs = new LinkedHashMap<>();
    private Map<String, Map<String, Double>> wordProbs = new LinkedHashMap<>();
    private List<String> classes = new ArrayList<>();

    public SymptomClassifier() {
        train();
    }

    private List<String> tokenize(String text) {
        List<String> tokens = new ArrayList<>();
        for (String kw : KEYWORDS) {
            if (text.contains(kw)) tokens.add(kw);
        }
        for (int i = 0; i < text.length() - 1; i++) {
            tokens.add(text.substring(i, i + 2));
        }
        return tokens;
    }

    private List<String[]> generateTrainingData() {
        return List.of(
            new String[]{"呕吐黄水不吃东西精神差","急性胃肠炎","高"},
            new String[]{"拉稀水样便便血","细小病毒/肠炎","高"},
            new String[]{"频繁呕吐白色泡沫","胃炎/胰腺炎","高"},
            new String[]{"咳嗽干咳发热精神差","犬窝咳/支气管炎","中"},
            new String[]{"打喷嚏流鼻涕眼屎多","上呼吸道感染","中"},
            new String[]{"皮肤红肿瘙痒掉毛","皮肤病/真菌感染","中"},
            new String[]{"耳朵有黑色分泌物摇头","耳螨/外耳炎","低"},
            new String[]{"牙齿发黄口臭牙结石","牙周病","低"},
            new String[]{"跛行不愿走动关节肿","关节炎/髋关节问题","中"},
            new String[]{"食欲不振消瘦精神萎靡","慢性病/寄生虫","中"},
            new String[]{"频繁排尿尿血排尿困难","尿路感染/结石","中"},
            new String[]{"眼泪多眼屎黏眼睛红肿","结膜炎/泪腺炎","低"},
            new String[]{"便秘腹胀排便困难","便秘/消化不良","低"},
            new String[]{"嗜睡不吃不喝体温高","感染/发热待查","高"},
            new String[]{"身体瘙痒有跳蚤黑点","跳蚤感染","低"},
            new String[]{"突然倒地抽搐口吐白沫","癫痫/中毒","高"},
            new String[]{"腹部胀大呼吸困难","心脏病/腹水","高"},
            new String[]{"掉毛严重有秃斑皮肤红","真菌/细菌性皮炎","中"},
            new String[]{"走路不稳原地转圈","耳部感染/神经问题","中"},
            new String[]{"鼻子干燥有脓性分泌物","鼻炎/犬瘟热","高"}
        );
    }

    private void train() {
        List<String[]> data = generateTrainingData();
        Set<String> classSet = new LinkedHashSet<>();
        for (String[] d : data) classSet.add(d[1]);
        classes = new ArrayList<>(classSet);

        Map<String, Integer> classCounts = new LinkedHashMap<>();
        Map<String, Map<String, Integer>> wordCounts = new LinkedHashMap<>();
        Map<String, Integer> totalWords = new LinkedHashMap<>();
        for (String c : classes) {
            classCounts.put(c, 0);
            wordCounts.put(c, new LinkedHashMap<>());
            totalWords.put(c, 0);
        }

        for (String[] d : data) {
            String text = d[0], disease = d[1];
            classCounts.merge(disease, 1, Integer::sum);
            List<String> tokens = tokenize(text);
            for (String t : tokens) {
                wordCounts.get(disease).merge(t, 1, Integer::sum);
                totalWords.merge(disease, 1, Integer::sum);
                vocab.putIfAbsent(t, vocab.size());
            }
        }

        int nDocs = data.size();
        for (String c : classes) {
            classProbs.put(c, (double) classCounts.get(c) / nDocs);
        }

        int vocabSize = vocab.size();
        for (String c : classes) {
            Map<String, Double> wp = new LinkedHashMap<>();
            wordProbs.put(c, wp);
            for (String w : vocab.keySet()) {
                int count = wordCounts.get(c).getOrDefault(w, 0);
                wp.put(w, (count + 1.0) / (totalWords.get(c) + vocabSize));
            }
        }
    }

    public List<Map<String, Object>> predict(String symptomsText, int topK) {
        List<String> tokens = tokenize(symptomsText);
        Map<String, Double> scores = new LinkedHashMap<>();

        for (String c : classes) {
            double logProb = Math.log(classProbs.getOrDefault(c, 1e-10));
            for (String t : tokens) {
                if (vocab.containsKey(t)) {
                    logProb += Math.log(wordProbs.getOrDefault(c, Collections.emptyMap()).getOrDefault(t, 1e-10));
                }
            }
            scores.put(c, Math.exp(logProb));
        }

        // Normalize
        double total = scores.values().stream().mapToDouble(Double::doubleValue).sum();
        if (total == 0) total = 1;

        // Severity map
        Map<String, String> severityMap = new LinkedHashMap<>();
        for (String[] d : generateTrainingData()) {
            severityMap.put(d[1], d[2]);
        }

        List<Map.Entry<String, Double>> sorted = new ArrayList<>(scores.entrySet());
        sorted.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));

        List<Map<String, Object>> output = new ArrayList<>();
        for (int i = 0; i < Math.min(topK, sorted.size()); i++) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("disease", sorted.get(i).getKey());
            item.put("probability", Math.round(sorted.get(i).getValue() / total * 1000.0) / 10.0);
            item.put("severity", severityMap.getOrDefault(sorted.get(i).getKey(), "中"));
            output.add(item);
        }
        return output;
    }
}
