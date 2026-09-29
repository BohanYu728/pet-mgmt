package com.petmgmt.ai.rag;

public record KnowledgeChunk(String id, String title, String text) {
    public KnowledgeChunk {
        if (id == null || id.isBlank() || title == null || title.isBlank() || text == null || text.isBlank()) {
            throw new IllegalArgumentException("知识片段不能为空");
        }
    }
}
