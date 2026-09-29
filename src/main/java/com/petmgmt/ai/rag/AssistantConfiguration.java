package com.petmgmt.ai.rag;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AssistantConfiguration {
    @Bean
    AssistantService assistantService(AiGateway gateway) {
        return new AssistantService(KnowledgeCatalog.load(), gateway);
    }
}
