package com.petmgmt.ai;

import java.net.URI;
import java.net.http.*;
import java.util.*;

/**
 * AI-powered pet health consultation using LLM API (DeepSeek/OpenAI compatible).
 * Falls back to keyword matching + ML symptom classifier when no API key configured.
 */
public class ConsultationAi {

    private static final String API_URL = System.getenv().getOrDefault("LLM_API_URL", "https://api.deepseek.com/v1/chat/completions");
    private static final String API_KEY = System.getenv().getOrDefault("DEEPSEEK_API_KEY", System.getenv().getOrDefault("LLM_API_KEY", ""));
    private static final String MODEL = System.getenv().getOrDefault("LLM_MODEL", "deepseek-chat");

    private static final String SYSTEM_PROMPT = """
        你是一位经验丰富的宠物医生（兽医），拥有20年临床经验。用户会描述宠物的症状，请你：
        1. 分析可能的疾病（列出2-3个最可能的，按可能性排序）
        2. 给出紧急程度判断（高/中/低）
        3. 提供详细的处理建议（居家护理 + 何时就医）
        注意：用中文回答，语气专业但温暖。始终提醒：你的建议仅供参考，严重情况需立即就医。回答控制在200-400字。纯文本格式。""";

    public static Map<String, Object> askAi(String species, String symptoms, String duration, List<String> knowledgeContext) {
        if (API_KEY.isEmpty()) return null;

        String speciesLabel = "dog".equals(species) ? "狗狗" : "猫咪";
        String durationText = duration != null && !duration.isEmpty() ? "，已经持续" + duration : "";
        String userMessage = "宠物种类：" + speciesLabel + "\n症状描述：" + symptoms + durationText + "\n请帮我分析可能是什么问题，该怎么处理。";

        try {
            StringBuilder msgs = new StringBuilder("[{\"role\":\"system\",\"content\":\"").append(escape(SYSTEM_PROMPT)).append("\"}");
            if (knowledgeContext != null && !knowledgeContext.isEmpty()) {
                String kbText = String.join("\\n", knowledgeContext.subList(0, Math.min(5, knowledgeContext.size())));
                msgs.append(",{\"role\":\"system\",\"content\":\"以下是相关知识库条目供参考：").append(escape(kbText)).append("\"}");
            }
            msgs.append(",{\"role\":\"user\",\"content\":\"").append(escape(userMessage)).append("\"}]");

            String body = "{\"model\":\"" + MODEL + "\",\"messages\":" + msgs + ",\"temperature\":0.7,\"max_tokens\":800}";

            HttpClient client = HttpClient.newHttpClient();
            HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + API_KEY)
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
            HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
            String content = extractContent(resp.body());

            if (content == null) return null;

            String severity = "中";
            if (content.contains("紧急程度：高") || content.contains("立即就医")) severity = "高";
            else if (content.contains("紧急程度：低")) severity = "低";

            String conditions = "AI 综合分析";
            for (String line : content.split("\n")) {
                if (line.contains("可能") && (line.contains("：") || line.contains(":"))) {
                    String[] parts = line.split("[：:]");
                    if (parts.length > 1) { conditions = parts[1].trim(); break; }
                }
            }

            return Map.of("advice", content, "matched_conditions", conditions,
                "severity", severity, "ai_generated", true);
        } catch (Exception e) {
            return null;
        }
    }

    private static String extractContent(String json) {
        try {
            int idx = json.indexOf("\"content\":\"");
            if (idx >= 0) {
                int start = idx + 11;
                int end = json.indexOf("\"", start);
                // Handle escaped quotes
                while (end > start && json.charAt(end - 1) == '\\') {
                    end = json.indexOf("\"", end + 1);
                }
                if (end > start) {
                    return json.substring(start, end)
                        .replace("\\n", "\n").replace("\\\"", "\"").replace("\\\\", "\\");
                }
            }
        } catch (Exception e) {}
        return null;
    }

    private static String escape(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "");
    }
}
