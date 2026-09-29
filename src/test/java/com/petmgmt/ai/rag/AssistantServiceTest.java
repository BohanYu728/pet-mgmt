package com.petmgmt.ai.rag;

import org.junit.jupiter.api.Test;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AssistantServiceTest {
    private final List<KnowledgeChunk> chunks = KnowledgeCatalog.parse("guide.md", "# 宠物管家\n## 疫苗提醒\n记录下次接种日期，系统显示提醒。\n## 商城\n用户可以浏览商品并管理购物车。");

    @Test void rejectsInvalidInputBeforeCallingGateway() {
        AssistantService service = new AssistantService(chunks, new FakeGateway(false));
        assertThrows(IllegalArgumentException.class, () -> service.ask("  "));
        assertThrows(IllegalArgumentException.class, () -> service.ask("x".repeat(1001)));
        assertThrows(IllegalArgumentException.class, () -> service.ask("hello\u0000world"));
    }

    @Test void missingCredentialsReturnsHonestLocalExcerpt() {
        var result = new AssistantService(chunks, new FakeGateway(false)).ask("疫苗提醒如何使用？");
        assertEquals("retrieval_only", result.status());
        assertTrue(result.answer().contains("系统显示提醒"));
        assertEquals("guide.md#疫苗提醒", result.sources().get(0).id());
    }

    @Test void rejectsHallucinatedCitationAndFallsBackToSource() {
        var result = new AssistantService(chunks, new FakeGateway(true, "虚构回答 [fake#id]")).ask("疫苗提醒如何使用？");
        assertEquals("retrieval_only", result.status());
        assertFalse(result.answer().contains("虚构回答"));
    }

    @Test void remoteFailureDoesNotPretendToBeGeneratedAnswer() {
        var gateway = new FakeGateway(true) {
            @Override public List<List<Double>> embed(List<String> texts) { throw new IllegalStateException("remote unavailable"); }
        };
        var result = new AssistantService(chunks, gateway).ask("疫苗提醒如何使用？");
        assertEquals("retrieval_only", result.status());
        assertTrue(result.answer().contains("资料原文"));
    }

    @Test void everyCitationMustMatchSelectedSource() {
        var result = new AssistantService(chunks, new FakeGateway(true,
                "记录日期 [guide.md#疫苗提醒]，另见 [private#file]。"))
                .ask("疫苗提醒如何使用？");
        assertEquals("retrieval_only", result.status());
    }

    @Test void longSectionsAreSplitIntoBoundedChunks() {
        var parsed = KnowledgeCatalog.parse("guide.md", "# 指南\n## 说明\n" + "项目功能说明。".repeat(120));
        assertTrue(parsed.size() > 1);
        assertTrue(parsed.stream().allMatch(c -> c.text().length() <= 450));
        assertEquals(parsed.size(), parsed.stream().map(KnowledgeChunk::id).distinct().count());
    }

    @Test void acceptsOnlyCitationsFromRetrievedChunks() {
        var result = new AssistantService(chunks, new FakeGateway(true, "请记录接种日期 [guide.md#疫苗提醒]。"))
                .ask("疫苗提醒如何使用？");
        assertEquals("answered", result.status());
        assertEquals(1, result.sources().size());
        assertEquals("guide.md#疫苗提醒", result.sources().get(0).id());
    }

    @Test void cosineRetrievalUsesEmbeddingRatherThanPosition() {
        var gateway = new FakeGateway(true, "可管理购物车 [guide.md#商城]。") {
            @Override public List<List<Double>> embed(List<String> texts) {
                if (texts.size() == 1) return List.of(List.of(0.0, 1.0));
                return List.of(List.of(1.0, 0.0), List.of(0.0, 1.0));
            }
        };
        var result = new AssistantService(chunks, gateway).ask("商城如何使用？");
        assertEquals("answered", result.status());
        assertEquals("guide.md#商城", result.sources().get(0).id());
    }

    static class FakeGateway implements AiGateway {
        final boolean available;
        final String answer;
        FakeGateway(boolean available) { this(available, ""); }
        FakeGateway(boolean available, String answer) { this.available = available; this.answer = answer; }
        public boolean available() { return available; }
        public List<List<Double>> embed(List<String> texts) {
            return texts.stream().map(t -> List.of(t.contains("商城") ? 0.0 : 1.0, t.contains("商城") ? 1.0 : 0.0)).toList();
        }
        public String answer(String question, List<KnowledgeChunk> context) { return answer; }
    }
}
