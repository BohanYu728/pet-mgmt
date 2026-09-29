package com.petmgmt.controller;

import com.petmgmt.ai.rag.AssistantService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/assistant")
public class AssistantController {
    private final AssistantService service;

    public AssistantController(AssistantService service) { this.service = service; }

    @PostMapping("/ask")
    public ResponseEntity<?> ask(@RequestBody(required = false) AskRequest request) {
        try {
            return ResponseEntity.ok(service.ask(request == null ? null : request.question()));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(new ErrorResponse("invalid_question", ex.getMessage()));
        }
    }

    public record AskRequest(String question) {}
    public record ErrorResponse(String status, String message) {}
}
