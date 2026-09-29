package com.petmgmt.ai.rag;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class OpenAiGatewayProtocolTest {
    private final ObjectMapper json = new ObjectMapper();
    private HttpServer server;
    private final List<JsonNode> embeddingRequests = new ArrayList<>();
    private final List<JsonNode> chatRequests = new ArrayList<>();

    @AfterEach void stop() { if (server != null) server.stop(0); }

    @Test void usesSdkProtocolAndRestoresEmbeddingOrderByIndex() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v1/embeddings", exchange -> {
            assertEquals("Bearer test-key", exchange.getRequestHeaders().getFirst("Authorization"));
            JsonNode request = json.readTree(exchange.getRequestBody());
            embeddingRequests.add(request);
            assertEquals("test-embedding", request.path("model").asText());
            int count = request.path("input").size();
            String body = count == 2
                    ? "{\"object\":\"list\",\"data\":[{\"object\":\"embedding\",\"index\":1,\"embedding\":[0,1]},{\"object\":\"embedding\",\"index\":0,\"embedding\":[1,0]}],\"model\":\"test-embedding\",\"usage\":{\"prompt_tokens\":2,\"total_tokens\":2}}"
                    : "{\"object\":\"list\",\"data\":[{\"object\":\"embedding\",\"index\":0,\"embedding\":[0,1]}],\"model\":\"test-embedding\",\"usage\":{\"prompt_tokens\":1,\"total_tokens\":1}}";
            respond(exchange, body);
        });
        server.createContext("/v1/chat/completions", exchange -> {
            assertEquals("Bearer test-key", exchange.getRequestHeaders().getFirst("Authorization"));
            JsonNode request = json.readTree(exchange.getRequestBody());
            chatRequests.add(request);
            respond(exchange, "{\"id\":\"chatcmpl-test\",\"object\":\"chat.completion\",\"created\":1,\"model\":\"test-chat\",\"choices\":[{\"index\":0,\"finish_reason\":\"stop\",\"message\":{\"role\":\"assistant\",\"content\":\"购物车可以管理商品 [guide.md#商城]。\"}}]}");
        });
        server.start();

        AiGateway gateway = new OpenAiGateway("test-key", "http://127.0.0.1:" + server.getAddress().getPort() + "/v1", "test-chat", "test-embedding");
        var chunks = List.of(new KnowledgeChunk("guide.md#健康", "健康", "健康记录"),
                new KnowledgeChunk("guide.md#商城", "商城", "商城购物车"));
        var result = new AssistantService(chunks, gateway).ask("商城购物车怎么使用？");

        assertEquals("answered", result.status());
        assertEquals("guide.md#商城", result.sources().get(0).id());
        assertEquals(2, embeddingRequests.size());
        assertEquals(1, chatRequests.size());
        JsonNode chat = chatRequests.get(0);
        assertEquals("test-chat", chat.path("model").asText());
        assertEquals("system", chat.path("messages").get(0).path("role").asText());
        assertEquals("user", chat.path("messages").get(1).path("role").asText());
        assertEquals("user", chat.path("messages").get(2).path("role").asText());
        assertTrue(chat.path("messages").get(1).path("content").asText().contains("guide.md#商城"));
    }

    private static void respond(HttpExchange exchange, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, bytes.length);
        try (var out = exchange.getResponseBody()) { out.write(bytes); }
    }
}
