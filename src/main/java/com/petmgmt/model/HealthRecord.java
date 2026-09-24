package com.petmgmt.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Entity
@Table(name = "health_records")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HealthRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pet_id", nullable = false)
    private Pet pet;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User creator;

    @Column(name = "record_type", nullable = false, length = 20)
    private String recordType;

    @Column(nullable = false, length = 256)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "record_date", nullable = false)
    private LocalDate recordDate;

    @Column(name = "next_due_date")
    private LocalDate nextDueDate;

    @Column(length = 128)
    private String veterinarian;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "attachment_path", length = 512)
    private String attachmentPath;

    @Column(name = "created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    @OneToMany(mappedBy = "healthRecord", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Reminder> reminders = new ArrayList<>();

    @PreUpdate
    protected void onUpdate() { this.updatedAt = LocalDateTime.now(); }

    public Map<String, Object> toDict() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", id);
        map.put("pet_id", pet != null ? pet.getId() : null);
        map.put("user_id", creator != null ? creator.getId() : null);
        map.put("record_type", recordType);
        map.put("title", title);
        map.put("description", description);
        map.put("record_date", recordDate != null ? recordDate.toString() : null);
        map.put("next_due_date", nextDueDate != null ? nextDueDate.toString() : null);
        map.put("veterinarian", veterinarian);
        map.put("notes", notes);
        map.put("pet_name", pet != null ? pet.getName() : null);
        map.put("created_at", createdAt != null ? createdAt.toString() : null);
        return map;
    }
}
