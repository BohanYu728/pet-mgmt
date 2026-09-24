package com.petmgmt.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Entity
@Table(name = "weight_records")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WeightRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pet_id", nullable = false)
    private Pet pet;

    @Column(nullable = false)
    private Double weight;

    @Column(name = "record_date", nullable = false)
    private LocalDate recordDate;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    public Map<String, Object> toDict() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", id); map.put("pet_id", pet != null ? pet.getId() : null);
        map.put("weight", weight);
        map.put("record_date", recordDate != null ? recordDate.toString() : null);
        map.put("notes", notes);
        map.put("created_at", createdAt != null ? createdAt.toString() : null);
        return map;
    }
}
