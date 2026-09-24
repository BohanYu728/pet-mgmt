package com.petmgmt.controller;

import com.petmgmt.dto.ApiResponse;
import com.petmgmt.model.User;
import com.petmgmt.repository.UserRepository;
import com.petmgmt.service.ConsultationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/consultation")
public class ConsultationController {

    private final ConsultationService consultationService;
    private final UserRepository userRepository;

    public ConsultationController(ConsultationService cs, UserRepository ur) {
        this.consultationService = cs; this.userRepository = ur;
    }

    @PostMapping("/ask")
    public ResponseEntity<?> ask(Authentication auth, @RequestBody Map<String, Object> data) {
        String species = (String) data.getOrDefault("species", "dog");
        String symptoms = (String) data.get("symptoms");
        if (symptoms == null || symptoms.isEmpty())
            return ResponseEntity.badRequest().body(ApiResponse.error("请描述症状"));

        Map<String, Object> advice = consultationService.getAdvice(species, symptoms);

        Long userId = auth != null ? (Long) auth.getPrincipal() : null;
        if (userId != null) {
            consultationService.saveConsultation(userId, species, symptoms,
                (String) data.get("duration"), advice,
                data.containsKey("pet_id") ? Long.valueOf(((Number) data.get("pet_id")).longValue()) : null);
        }

        return ResponseEntity.ok(advice);
    }

    @GetMapping("/history")
    public ResponseEntity<?> history(Authentication auth, @RequestParam(defaultValue = "1") int page) {
        if (auth == null) return ResponseEntity.status(401).body(ApiResponse.error("请先登录"));
        return ResponseEntity.ok(consultationService.getConsultationHistory((Long) auth.getPrincipal(), page));
    }
}
