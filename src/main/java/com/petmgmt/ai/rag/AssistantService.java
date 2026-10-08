package com.petmgmt.ai.rag;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

public class AssistantService {
    private static final Pattern CITATION = Pattern.compile("\\[([^]\\r\\n]{1,100})\\]");
    private final List<KnowledgeChunk> chunks;
    private final AiGateway gateway;
    private volatile List<List<Double>> cachedEmbeddings;

    public AssistantService(List<KnowledgeChunk> chunks, AiGateway gateway) {
        this.chunks = chunks.stream().filter(c -> AiSafetyGuard.inspect(c.title()+"。"+c.text()) == null).toList();
        this.gateway = gateway;
    }

    public AskResult ask(String question) {
        if (question == null || question.isBlank() || question.length() > 1000 ||
                question.chars().anyMatch(c -> Character.isISOControl(c) && c != '\n')) {
            throw new IllegalArgumentException("问题须为1至1000字的普通文本");
        }
        String clean = question.trim();
        String risk = AiSafetyGuard.inspect(clean);
        if (risk != null) return new AskResult(risk, List.of(), "safety_blocked",
                new Safety(true, "input_blocked", risk));
        List<KnowledgeChunk> selected;
        if (!gateway.available()) {
            selected = lexical(clean);
            return fallback(selected);
        }
        try {
            selected = vector(clean);
            if (selected.isEmpty()) return new AskResult("资料中没有找到相关内容。", List.of(), "no_sources");
            String answer = gateway.answer(clean, selected);
            if (answer != null && AiSafetyGuard.inspect(answer) != null) {
                AskResult safe = fallback(selected);
                return new AskResult(safe.answer(), safe.sources(), safe.status(),
                        new Safety(true, "output_blocked", "已隐藏包含风险内容的模型回答，仅展示已筛选的项目资料。"));
            }
            List<Source> cited = citedSources(answer, selected);
            if (cited.isEmpty()) return fallback(selected);
            return new AskResult(answer, cited, "answered");
        } catch (RuntimeException ex) {
            return fallback(lexical(clean));
        }
    }

    private List<KnowledgeChunk> vector(String question) {
        List<List<Double>> indexed = cachedEmbeddings;
        if (indexed == null) {
            synchronized (this) {
                indexed = cachedEmbeddings;
                if (indexed == null) {
                    indexed = gateway.embed(chunks.stream().map(c -> c.title() + "。" + c.text()).toList());
                    if (indexed.size() != chunks.size()) throw new IllegalStateException("知识向量数量不符");
                    cachedEmbeddings = indexed;
                }
            }
        }
        List<List<Double>> query = gateway.embed(List.of(question));
        if (query.size() != 1) throw new IllegalStateException("问题向量数量不符");
        var q = query.get(0);
        var scores = new ArrayList<Scored>();
        for (int i = 0; i < chunks.size(); i++) {
            double similarity = cosine(q, indexed.get(i));
            if (Double.isFinite(similarity) && similarity > 0.15) scores.add(new Scored(chunks.get(i), similarity));
        }
        return scores.stream().sorted(Comparator.comparingDouble(Scored::score).reversed())
                .limit(3).map(Scored::chunk).toList();
    }

    static double cosine(List<Double> a, List<Double> b) {
        if (a.isEmpty() || a.size() != b.size()) throw new IllegalArgumentException("向量维度不符");
        double dot = 0, aa = 0, bb = 0;
        for (int i = 0; i < a.size(); i++) {
            double x = a.get(i), y = b.get(i);
            if (!Double.isFinite(x) || !Double.isFinite(y)) throw new IllegalArgumentException("无效向量");
            dot += x * y; aa += x * x; bb += y * y;
        }
        return aa == 0 || bb == 0 ? 0 : dot / Math.sqrt(aa * bb);
    }

    private List<KnowledgeChunk> lexical(String question) {
        var scores = new ArrayList<Scored>();
        String normalized = question.replaceAll("[\\p{P}\\p{Z}\\s]", "");
        for (var chunk : chunks) {
            String content = chunk.title() + chunk.text();
            double score = 0;
            for (int i = 0; i + 2 <= normalized.length(); i++) {
                if (content.contains(normalized.substring(i, i + 2))) score++;
            }
            if (score > 0) scores.add(new Scored(chunk, score));
        }
        return scores.stream().sorted(Comparator.comparingDouble(Scored::score).reversed())
                .limit(3).map(Scored::chunk).toList();
    }

    private List<Source> citedSources(String answer, List<KnowledgeChunk> selected) {
        if (answer == null || answer.isBlank() || answer.length() > 3000) return List.of();
        Set<String> citations = new HashSet<>();
        var matcher = CITATION.matcher(answer);
        while (matcher.find()) citations.add(matcher.group(1));
        if (citations.isEmpty() || citations.stream().anyMatch(id -> selected.stream().noneMatch(c -> c.id().equals(id)))) {
            return List.of();
        }
        return selected.stream().filter(c -> citations.contains(c.id())).map(Source::from).toList();
    }

    private AskResult fallback(List<KnowledgeChunk> selected) {
        if (selected.isEmpty()) return new AskResult("资料中没有找到相关内容。", List.of(), "no_sources");
        var first = selected.get(0);
        return new AskResult("目前无法生成经核验的智能回答。以下是项目资料原文：" + first.text() + " [" + first.id() + "]",
                List.of(Source.from(first)), "retrieval_only");
    }

    private record Scored(KnowledgeChunk chunk, double score) {}
    public record Source(String id, String title, String excerpt) {
        static Source from(KnowledgeChunk chunk) { return new Source(chunk.id(), chunk.title(), chunk.text()); }
    }
    public record Safety(boolean enabled, String decision, String reason) {}
    public record AskResult(String answer, List<Source> sources, String status, Safety safety) {
        public AskResult(String answer, List<Source> sources, String status) {
            this(answer, sources, status, new Safety(true, "passed", "已完成规则检查；不代表完全安全，回答仍需核对。"));
        }
    }
}
