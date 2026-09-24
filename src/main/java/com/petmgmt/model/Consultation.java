package com.petmgmt.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Entity
@Table(name = "consultations")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Consultation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pet_id")
    private Pet pet;

    @Column(nullable = false, length = 10)
    @Builder.Default
    private String species = "dog";

    @Column(nullable = false, columnDefinition = "TEXT")
    private String symptoms;

    @Column(length = 64)
    private String duration;

    @Column(columnDefinition = "TEXT")
    private String advice;

    @Column(name = "matched_conditions", length = 256)
    private String matchedConditions;

    @Column(length = 10)
    @Builder.Default
    private String severity = "低";

    @Column(name = "created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    public Map<String, Object> toDict() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", id);
        map.put("user_id", user != null ? user.getId() : null);
        map.put("pet_id", pet != null ? pet.getId() : null);
        map.put("species", species);
        map.put("symptoms", symptoms);
        map.put("duration", duration);
        map.put("advice", advice);
        map.put("matched_conditions", matchedConditions);
        map.put("severity", severity);
        map.put("pet_name", pet != null ? pet.getName() : null);
        map.put("created_at", createdAt != null ? createdAt.toString() : null);
        return map;
    }
}
