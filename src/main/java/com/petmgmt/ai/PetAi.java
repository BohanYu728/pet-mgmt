package com.petmgmt.ai;

import java.net.URI;
import java.net.http.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * AI pet health reports, encyclopedia Q&A, and product recommendations.
 * Uses external LLM API (DeepSeek compatible) with local knowledge base fallback.
 */
public class PetAi {

    private static final String API_URL = System.getenv().getOrDefault("LLM_API_URL", "https://api.deepseek.com/v1/chat/completions");
    private static final String API_KEY = System.getenv().getOrDefault("DEEPSEEK_API_KEY", System.getenv().getOrDefault("LLM_API_KEY", ""));
    private static final String MODEL = System.getenv().getOrDefault("LLM_MODEL", "deepseek-chat");

    private static final Map<String, String> BREED_RISKS = Map.ofEntries(
        Map.entry("金毛","髋关节发育不良、皮肤病、肿瘤"),
        Map.entry("拉布拉多","肥胖、髋关节问题、眼部疾病"),
        Map.entry("泰迪","髌骨脱位、泪痕、牙齿问题"),
        Map.entry("哈士奇","肠胃敏感、角膜疾病、锌缺乏"),
        Map.entry("柯基","椎间盘疾病、肥胖、髋关节问题"),
        Map.entry("柴犬","过敏、甲状腺问题、青光眼"),
        Map.entry("德牧","髋关节问题、胃扭转、脊椎疾病"),
        Map.entry("比熊","泪痕、皮肤过敏、髌骨脱位"),
        Map.entry("法斗","呼吸问题、皮肤病、脊椎异常"),
        Map.entry("英短","心肌病、多囊肾、肥胖"),
        Map.entry("美短","肥厚型心肌病、口腔问题"),
        Map.entry("布偶","肥厚型心肌病、膀胱结石"),
        Map.entry("暹罗","呼吸道问题、牙龈炎、眼疾"),
        Map.entry("橘猫","肥胖、糖尿病、泌尿问题")
    );

    // ---- LLM API ----
    private static String callLlm(String systemPrompt, String userMessage) {
        if (API_KEY.isEmpty()) return null;
        try {
            String body = String.format(
                "{\"model\":\"%s\",\"messages\":[{\"role\":\"system\",\"content\":\"%s\"},{\"role\":\"user\",\"content\":\"%s\"}],\"temperature\":0.7,\"max_tokens\":800}",
                MODEL, escapeJson(systemPrompt), escapeJson(userMessage));
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + API_KEY)
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
            HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
            // Simple JSON extraction
            String content = resp.body();
            int idx = content.indexOf("\"content\":\"");
            if (idx >= 0) {
                int start = idx + 11;
                int end = content.indexOf("\"", start);
                if (end > start) return content.substring(start, end)
                    .replace("\\n", "\n").replace("\\\"", "\"");
            }
            return content;
        } catch (Exception e) {
            return null;
        }
    }

    private static String escapeJson(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
    }

    // ---- Health Report ----
    public static String generateHealthReport(String petName, String species, String breed,
            Object age, Object weight, String gender, List<Map<String, Object>> healthRecords,
            List<Map<String, Object>> weightHistory) {
        String speciesCn = "dog".equals(species) ? "狗狗" : "猫咪";
        String genderCn = switch (gender != null ? gender : "unknown") {
            case "male" -> "公"; case "female" -> "母"; default -> "未知";
        };

        StringBuilder recordsText = new StringBuilder();
        for (Map<String, Object> r : healthRecords != null ? healthRecords : Collections.<Map<String, Object>>emptyList()) {
            recordsText.append("- ").append(r.getOrDefault("record_type","")).append(": ")
                .append(r.getOrDefault("title","")).append(" (")
                .append(r.getOrDefault("record_date","")).append(")\n");
        }

        StringBuilder weightText = new StringBuilder();
        for (Map<String, Object> w : weightHistory != null ? weightHistory : Collections.<Map<String, Object>>emptyList()) {
            weightText.append("- ").append(w.getOrDefault("record_date","")).append(": ")
                .append(w.getOrDefault("weight","")).append("kg\n");
        }
        if (weightText.isEmpty()) weightText.append("当前体重: ").append(weight).append("kg\n");

        String systemPrompt = "你是资深宠物健康顾问。根据宠物的品种、年龄、体重、健康记录，生成一份全面的健康分析报告。用中文回答，纯文本格式，控制在300-500字。";
        String userMsg = String.format("宠物信息：\n名称：%s\n种类：%s\n品种：%s\n性别：%s\n年龄：%s岁\n体重：%skg\n\n体重历史记录：\n%s\n健康记录：\n%s\n请生成健康分析报告。",
            petName, speciesCn, breed != null ? breed : "未知", genderCn, age != null ? age : "未知",
            weight != null ? weight : "未知", weightText, !recordsText.isEmpty() ? recordsText : "暂无健康记录");

        String result = callLlm(systemPrompt, userMsg);
        if (result != null) return result;

        // Fallback: ML-based report
        return mlHealthReport(petName, speciesCn, species, breed, age, weight, gender, healthRecords, weightHistory);
    }

    private static String mlHealthReport(String petName, String speciesCn, String species, String breed,
            Object ageObj, Object weightObj, String gender, List<Map<String, Object>> healthRecords,
            List<Map<String, Object>> weightHistory) {
        double age = toDouble(ageObj, 3);
        double weight = toDouble(weightObj, 10);
        String genderCn = "male".equals(gender) ? "公" : "female".equals(gender) ? "母" : "未知";

        int vaccineCount = countType(healthRecords, "vaccine");
        int dewormingCount = countType(healthRecords, "deworming");
        int checkupCount = countType(healthRecords, "checkup");

        int weightTrend = 0;
        if (weightHistory != null && weightHistory.size() >= 2) {
            double first = toDouble(weightHistory.get(0).get("weight"), 0);
            double last = toDouble(weightHistory.get(weightHistory.size() - 1).get("weight"), 0);
            weightTrend = Double.compare(last, first);
        }

        int breedRisk = 0;
        if (breed != null) {
            for (String kw : List.of("金毛","拉布拉多","哈士奇","柯基","德牧","法斗","英短","美短","布偶")) {
                if (breed.contains(kw)) { breedRisk = 1; break; }
            }
        }

        HealthPredictor hp = new HealthPredictor();
        double mlScore = hp.predict("dog".equals(species) ? 0 : 1, age, weight,
            vaccineCount, dewormingCount, checkupCount, weightTrend, breedRisk);

        String ageStage, ageAdvice;
        if (age < 1) { ageStage = "幼年期"; ageAdvice = "处于生长发育关键期，需高蛋白高钙饮食"; }
        else if (age < 7) { ageStage = "成年期"; ageAdvice = "身体状况稳定，保持年度体检"; }
        else { ageStage = "老年期"; ageAdvice = "进入中老年，建议每半年体检一次"; }

        String wstatus = "dog".equals(species)
            ? (weight < 5 ? "偏瘦" : weight > 30 ? "偏重" : weight > 25 ? "略重" : "正常")
            : (weight < 2.5 ? "偏瘦" : weight > 6 ? "偏重" : "正常");

        String riskText = "";
        if (breed != null) {
            for (var entry : BREED_RISKS.entrySet()) {
                if (breed.contains(entry.getKey())) { riskText = entry.getValue(); break; }
            }
        }

        String exAdvice = "dog".equals(species) && weight > 10
            ? "每日30-60分钟户外活动" : "dog".equals(species)
            ? "每日20-30分钟散步" : "提供攀爬玩具，每日互动玩耍15-20分钟";

        return String.format("""
            【%s 健康分析报告】

            ▎ML 模型预测健康评分：%s/10

            ▎基本信息
            品种：%s · %s · 性别：%s
            年龄：%s岁 · 阶段：%s · 体重：%skg (%s)

            ▎保健记录分析
            疫苗：%d次 · 驱虫：%d次 · 体检：%d次
            %s

            ▎品种遗传风险：%s

            ▎饮食建议
            %s

            ▎运动建议
            %s
            %s

            ⚠ ML模型基于500条合成宠物健康数据训练，预测评分仅供参考。如有疑虑请就医。""",
            petName, mlScore, breed != null ? breed : "未知", speciesCn, genderCn,
            age, ageStage, weight, wstatus,
            vaccineCount, dewormingCount, checkupCount,
            (vaccineCount + dewormingCount + checkupCount >= 4) ? "记录完善，保健意识良好" : "建议补充缺失的保健项目",
            !riskText.isEmpty() ? riskText : "无特定品种遗传风险",
            age < 1 ? "幼年需少食多餐（3-4次/天）" : "定时定量（2次/天）",
            exAdvice, ageAdvice);
    }

    // ---- Encyclopedia Q&A ----
    private static final Map<String, Map<String, String>> KB = buildKnowledgeBase();

    public static String askEncyclopedia(String question) {
        String systemPrompt = "你是宠物百科专家，精通犬猫饲养、健康、行为等所有方面。中文简洁回答150-300字。";
        String result = callLlm(systemPrompt, question);
        if (result != null) return result;

        // Local KB search
        for (var category : KB.values()) {
            for (var entry : category.entrySet()) {
                if (question.contains(entry.getKey()) || entry.getKey().contains(question)) {
                    return entry.getValue();
                }
            }
        }
        return "建议您从以下几个方面关注宠物健康：1) 饮食营养均衡 2) 定期疫苗接种和驱虫 3) 适量运动 4) 定期体检。如果问题持续，请咨询专业兽医。";
    }

    // ---- Product Recommendations ----
    public static String recommendProducts(String petName, String species, String breed,
            Object age, Object weight, List<Map<String, Object>> products) {
        String speciesCn = "dog".equals(species) ? "狗狗" : "猫咪";
        if (products == null || products.isEmpty()) {
            return String.format("商城暂无适合%s的商品，请联系管理员上架相关产品。", speciesCn);
        }

        // Use ML recommender
        ProductRecommender rec = new ProductRecommender();
        List<Map<String, Object>> recs = rec.recommend(species, toDouble(age, 3), toDouble(weight, 10), products, 5);

        if (recs.isEmpty()) {
            return String.format("😔 暂时没有找到完美匹配 %s（%s）的商品，请前往商城浏览。", petName, speciesCn);
        }

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("🤖 ML 模型为 %s（%s，%s岁，%skg）推荐：\n\n", petName, speciesCn, age, weight));
        int i = 1;
        for (Map<String, Object> p : recs) {
            String icon = "food".equals(p.get("category")) ? "🍖" : "🎾";
            sb.append(String.format("%d. %s %s %s — ¥%s\n   匹配度 %.0f%% · %s · %s\n",
                i++, i == 2 ? "⭐" : "  ", icon, p.get("name"),
                String.format("%.2f", ((Number) p.get("price")).doubleValue()),
                ((Number) p.getOrDefault("similarity", 0.0)).doubleValue() * 100,
                p.getOrDefault("category_display", ""),
                p.getOrDefault("target_species_display", "")));
        }
        sb.append("\n💡 基于余弦相似度算法计算，综合考虑品种、年龄、体重等因素。");
        return sb.toString();
    }

    // ---- Helpers ----
    private static double toDouble(Object obj, double def) {
        if (obj instanceof Number n) return n.doubleValue();
        if (obj instanceof String s) {
            try { return Double.parseDouble(s); } catch (NumberFormatException e) {}
        }
        return def;
    }

    private static int countType(List<Map<String, Object>> records, String type) {
        if (records == null) return 0;
        return (int) records.stream().filter(r -> type.equals(r.get("record_type"))).count();
    }

    private static Map<String, Map<String, String>> buildKnowledgeBase() {
        Map<String, Map<String, String>> kb = new LinkedHashMap<>();

        Map<String, String> feeding = new LinkedHashMap<>();
        feeding.put("幼犬一天喂几次", "幼犬（2-6月龄）建议每天喂3-4次，少量多餐。6月龄后逐渐减少到每天2次。");
        feeding.put("猫咪一天喂几次", "成猫一般每天喂2-3次。干粮可自由采食但需控制总量，湿粮建议定时喂食。");
        feeding.put("狗狗不能吃什么", "狗狗禁食清单：巧克力、洋葱、大蒜、葡萄/葡萄干、木糖醇、酒精、咖啡因等。");
        kb.put("喂食", feeding);

        Map<String, String> health = new LinkedHashMap<>();
        health.put("狗狗吐黄水", "吐黄色液体（胆汁）可能原因：空腹时间过长、轻微胃炎。如持续呕吐需就医。");
        health.put("狗狗拉稀怎么办", "轻微腹泻可先禁食12-24小时。持续腹泻超过24小时需立即就医。");
        kb.put("健康", health);

        Map<String, String> behavior = new LinkedHashMap<>();
        behavior.put("狗狗乱咬东西", "原因：换牙期、精力过剩、分离焦虑。提供磨牙玩具、增加运动量。");
        behavior.put("猫咪半夜吵醒主人", "猫咪是晨昏性动物，睡前陪猫充分玩耍消耗精力可改善。");
        kb.put("行为", behavior);

        return kb;
    }
}
