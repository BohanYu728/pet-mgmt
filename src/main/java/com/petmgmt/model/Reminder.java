package com.petmgmt.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Entity
@Table(name = "reminders")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Reminder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pet_id", nullable = false)
    private Pet pet;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "health_record_id")
    private HealthRecord healthRecord;

    @Column(nullable = false, length = 256)
    private String title;

    @Column(name = "reminder_type", nullable = false, length = 20)
    private String reminderType;

    @Column(name = "reminder_date", nullable = false)
    private LocalDate reminderDate;

    @Column(name = "is_read")
    private Boolean isRead = false;

    @Column(name = "is_dismissed")
    private Boolean isDismissed = false;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    public Map<String, Object> toDict() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", id);
        map.put("user_id", user != null ? user.getId() : null);
        map.put("pet_id", pet != null ? pet.getId() : null);
        map.put("health_record_id", healthRecord != null ? healthRecord.getId() : null);
        map.put("title", title);
        map.put("reminder_type", reminderType);
        map.put("reminder_date", reminderDate != null ? reminderDate.toString() : null);
        map.put("is_read", isRead);
        map.put("is_dismissed", isDismissed);
        map.put("pet_name", pet != null ? pet.getName() : null);
        map.put("pet_avatar", pet != null ? pet.getAvatar() : null);
        map.put("created_at", createdAt != null ? createdAt.toString() : null);
        return map;
    }
}
