package com.petmgmt.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.*;

@Entity
@Table(name = "emotion_history")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmotionHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pet_id", nullable = false)
    private Pet pet;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 32)
    private String emotion;

    private Double confidence;

    @Column(columnDefinition = "TEXT")
    private String comfortAdvice;

    @Column(name = "image_path", length = 512)
    private String imagePath;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    public Map<String, Object> toDict() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", id); map.put("pet_id", pet != null ? pet.getId() : null);
        map.put("user_id", user != null ? user.getId() : null);
        map.put("emotion", emotion); map.put("confidence", confidence);
        map.put("comfort_advice", comfortAdvice); map.put("image_path", imagePath);
        map.put("pet_name", pet != null ? pet.getName() : null);
        map.put("created_at", createdAt != null ? createdAt.toString() : null);
        return map;
    }
}
