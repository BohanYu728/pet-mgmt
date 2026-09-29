package com.petmgmt.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.*;

@Entity
@Table(name = "products")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 128)
    private String name;

    @Column(nullable = false, length = 10)
    @Builder.Default
    private String category = "food";

    @Column(name = "target_species", nullable = false, length = 10)
    @Builder.Default
    private String targetSpecies = "both";

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    @Builder.Default
    private Double price = 0.0;

    @Column(nullable = false)
    @Builder.Default
    private Integer stock = 0;

    @Column(length = 512)
    private String image;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "merchant_id")
    private User merchant;

    @Column(name = "created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate
    protected void onUpdate() { this.updatedAt = LocalDateTime.now(); }

    public Map<String, Object> toDict() {
        String speciesDisplay = "dog".equals(targetSpecies) ? "狗狗专用" :
                "cat".equals(targetSpecies) ? "猫咪专用" : "猫狗通用";
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", id);
        map.put("name", name);
        map.put("category", category);
        map.put("category_display", switch (category) {
            case "food" -> "宠物食品";
            case "toy" -> "宠物玩具";
            case "accessory" -> "用品配件";
            case "medicine" -> "健康护理";
            default -> "其他";
        });
        map.put("target_species", targetSpecies);
        map.put("target_species_display", speciesDisplay);
        map.put("description", description);
        map.put("price", price);
        map.put("stock", stock);
        map.put("image", image);
        map.put("is_active", isActive);
        map.put("merchant_id", merchant != null ? merchant.getId() : null);
        map.put("merchant_name", merchant != null ? merchant.getStoreName() : null);
        map.put("created_at", createdAt != null ? createdAt.toString() : null);
        map.put("updated_at", updatedAt != null ? updatedAt.toString() : null);
        return map;
    }
}
