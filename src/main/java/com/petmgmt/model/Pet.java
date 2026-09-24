package com.petmgmt.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Entity
@Table(name = "pets")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Pet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User owner;

    @Column(nullable = false, length = 64)
    private String name;

    @Column(nullable = false, length = 10)
    private String species = "dog";

    @Column(length = 128)
    private String breed;

    @Column(length = 10)
    private String gender = "unknown";

    @Column(name = "birth_date")
    private LocalDate birthDate;

    private BigDecimal age;

    private BigDecimal weight;

    @Column(length = 64)
    private String color;

    @Column(length = 512)
    private String avatar;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    @OneToMany(mappedBy = "pet", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<HealthRecord> healthRecords = new ArrayList<>();

    @OneToMany(mappedBy = "pet", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Reminder> reminders = new ArrayList<>();

    @OneToMany(mappedBy = "pet", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<EmotionHistory> emotions = new ArrayList<>();

    @PreUpdate
    protected void onUpdate() { this.updatedAt = LocalDateTime.now(); }

    public Double getComputedAge() {
        if (birthDate == null) return null;
        long months = ChronoUnit.MONTHS.between(birthDate, LocalDate.now());
        return Math.round(months / 12.0 * 10.0) / 10.0;
    }

    public String getAgeDisplay() {
        if (birthDate == null) return null;
        long totalMonths = ChronoUnit.MONTHS.between(birthDate, LocalDate.now());
        long years = totalMonths / 12;
        long months = totalMonths % 12;
        if (years < 1) return months + "个月";
        if (months == 0) return years + "岁";
        return years + "岁" + months + "个月";
    }

    public Map<String, Object> toDict() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", id);
        map.put("user_id", owner != null ? owner.getId() : null);
        map.put("name", name);
        map.put("species", species);
        map.put("breed", breed);
        map.put("gender", gender);
        map.put("birth_date", birthDate != null ? birthDate.toString() : null);
        Double computed = getComputedAge();
        map.put("age", computed != null ? computed : age);
        map.put("age_display", getAgeDisplay());
        map.put("weight", weight);
        map.put("color", color);
        map.put("avatar", avatar);
        map.put("description", description);
        map.put("created_at", createdAt != null ? createdAt.toString() : null);
        return map;
    }
}
