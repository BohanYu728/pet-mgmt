package com.petmgmt.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.*;

@Entity
@Table(name = "hospitals")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Hospital {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 128)
    private String name;

    @Column(nullable = false, length = 256)
    private String address;

    @Column(length = 32)
    private String phone;

    @Column(nullable = false)
    private Double latitude;

    @Column(nullable = false)
    private Double longitude;

    @Column(length = 256)
    private String services;

    @Column(length = 128)
    private String hours;

    @Column(name = "image_url", length = 512)
    private String imageUrl;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @OneToMany(mappedBy = "hospital", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Review> reviews = new ArrayList<>();

    public Map<String, Object> toDict() {
        Double avgRating = null;
        if (!reviews.isEmpty()) {
            avgRating = reviews.stream().mapToInt(Review::getRating).average().orElse(0);
            avgRating = Math.round(avgRating * 10.0) / 10.0;
        }
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", id);
        map.put("name", name);
        map.put("address", address);
        map.put("phone", phone);
        map.put("latitude", latitude);
        map.put("longitude", longitude);
        map.put("services", services);
        map.put("hours", hours);
        map.put("image_url", imageUrl);
        map.put("avg_rating", avgRating);
        map.put("review_count", reviews.size());
        map.put("created_at", createdAt != null ? createdAt.toString() : null);
        return map;
    }
}
