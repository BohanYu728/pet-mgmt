package com.petmgmt.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.*;

@Entity
@Table(name = "breed_history")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BreedHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pet_id")
    private Pet pet;

    @Column(name = "image_path", length = 512)
    private String imagePath;

    @Column(name = "recognized_breed", nullable = false, length = 128)
    private String recognizedBreed;

    @Column(nullable = false)
    private Double confidence;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(columnDefinition = "TEXT")
    private String careTips;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    public Map<String, Object> toDict() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", id); map.put("user_id", user != null ? user.getId() : null);
        map.put("pet_id", pet != null ? pet.getId() : null);
        map.put("image_path", imagePath); map.put("recognized_breed", recognizedBreed);
        map.put("confidence", confidence); map.put("description", description);
        map.put("care_tips", careTips);
        map.put("created_at", createdAt != null ? createdAt.toString() : null);
        return map;
    }
}
