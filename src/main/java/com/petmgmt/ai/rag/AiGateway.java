package com.petmgmt.ai.rag;

import java.util.List;

public interface AiGateway {
    boolean available();
    List<List<Double>> embed(List<String> texts);
    String answer(String question, List<KnowledgeChunk> context);
}
