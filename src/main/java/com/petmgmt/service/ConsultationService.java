package com.petmgmt.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.petmgmt.ai.ConsultationAi;
import com.petmgmt.ai.SymptomClassifier;
import com.petmgmt.model.*;
import com.petmgmt.repository.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.util.*;

@Service
public class ConsultationService {

    private final ConsultationRepository consultationRepository;

    public ConsultationService(ConsultationRepository consultationRepository) {
        this.consultationRepository = consultationRepository;
    }

    public Map<String, Object> getAdvice(String species, String symptoms) {
        // 1. Try AI
        Map<String, Object> aiResult = ConsultationAi.askAi(species, symptoms, null, null);
        if (aiResult != null) return aiResult;

        // 2. Try ML symptom classifier
        SymptomClassifier sc = new SymptomClassifier();
        List<Map<String, Object>> mlResults = sc.predict(symptoms, 3);
        if (!mlResults.isEmpty()) {
            Map<String, Object> top = mlResults.get(0);
            StringBuilder advice = new StringBuilder();
            advice.append("可能的疾病：").append(top.get("disease")).append("（")
                .append(top.get("probability")).append("%）\n");
            advice.append("紧急程度：").append(top.get("severity")).append("\n\n");
            advice.append("建议：请根据症状严重程度及时就医。如症状持续或加重，建议立即前往宠物医院。");

            return Map.of("advice", advice.toString(),
                "matched_conditions", (String) top.get("disease"),
                "severity", (String) top.get("severity"),
                "ml_predicted", true);
        }

        // 3. Keyword fallback
        return Map.of("advice", "请详细描述宠物的症状，包括是否呕吐、腹泻、精神状态、食欲等。如有紧急情况请立即就医。",
            "matched_conditions", "需进一步了解", "severity", "中");
    }

    @Transactional
    public Map<String, Object> saveConsultation(Long userId, String species, String symptoms,
                                                  String duration, Map<String, Object> adviceData, Long petId) {
        Consultation c = new Consultation();
        c.setUser(new User()); c.getUser().setId(userId);
        c.setSpecies(species); c.setSymptoms(symptoms); c.setDuration(duration);
        c.setAdvice((String) adviceData.get("advice"));
        c.setMatchedConditions((String) adviceData.get("matched_conditions"));
        c.setSeverity((String) adviceData.get("severity"));
        consultationRepository.save(c);
        return c.toDict();
    }

    public Map<String, Object> getConsultationHistory(Long userId, int page) {
        PageRequest pr = PageRequest.of(page - 1, 10);
        Page<Consultation> result = consultationRepository.findByUserIdOrderByCreatedAtDesc(userId, pr);
        return Map.of("records", result.getContent().stream().map(Consultation::toDict).toList(),
            "page", page, "pages", result.getTotalPages(), "has_next", result.hasNext(), "has_prev", result.hasPrevious());
    }
}
