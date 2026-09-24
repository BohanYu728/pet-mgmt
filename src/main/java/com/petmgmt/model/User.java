package com.petmgmt.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 64)
    private String username;

    @Column(nullable = false, unique = true, length = 128)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 256)
    private String passwordHash;

    @Column(length = 20)
    private String phone;

    @Column(length = 512)
    private String avatar;

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String role = "user";

    @Column(name = "is_admin")
    @Builder.Default
    private Boolean isAdmin = false;

    @Column(name = "store_name", length = 128)
    private String storeName;

    @Column(name = "store_phone", length = 20)
    private String storePhone;

    @Column(name = "rent_amount")
    @Builder.Default
    private Double rentAmount = 0.0;

    @Column(name = "rent_due_day")
    @Builder.Default
    private Integer rentDueDay = 1;

    @Column(name = "last_rent_paid")
    private LocalDateTime lastRentPaid;

    @Column(name = "is_approved")
    @Builder.Default
    private Boolean isApproved = true;

    @Column(name = "created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    @OneToMany(mappedBy = "owner", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Pet> pets = new ArrayList<>();

    @OneToMany(mappedBy = "creator")
    @Builder.Default
    private List<HealthRecord> healthRecords = new ArrayList<>();

    @OneToMany(mappedBy = "user")
    @Builder.Default
    private List<Reminder> reminders = new ArrayList<>();

    @OneToMany(mappedBy = "user")
    @Builder.Default
    private List<EmotionHistory> emotions = new ArrayList<>();

    @OneToMany(mappedBy = "user")
    @Builder.Default
    private List<BreedHistory> breedHistories = new ArrayList<>();

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public Map<String, Object> toDict() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", id);
        map.put("username", username);
        map.put("email", email);
        map.put("phone", phone);
        map.put("avatar", avatar);
        map.put("role", role);
        map.put("is_admin", "admin".equals(role));
        map.put("store_name", storeName);
        map.put("store_phone", storePhone);
        map.put("rent_amount", rentAmount);
        map.put("rent_due_day", rentDueDay);
        map.put("last_rent_paid", lastRentPaid != null ? lastRentPaid.toString() : null);
        map.put("is_approved", isApproved);
        map.put("created_at", createdAt != null ? createdAt.toString() : null);
        return map;
    }
}
