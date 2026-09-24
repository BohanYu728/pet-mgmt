package com.petmgmt.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Entity
@Table(name = "orders")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String status = "pending";

    @Column(name = "total_amount", nullable = false)
    @Builder.Default
    private Double totalAmount = 0.0;

    @Column(columnDefinition = "TEXT")
    private String note;

    @Column(name = "recipient_name", length = 64)
    private String recipientName;

    @Column(name = "recipient_phone", length = 20)
    private String recipientPhone;

    @Column(name = "address_province", length = 32)
    private String addressProvince;

    @Column(name = "address_city", length = 32)
    private String addressCity;

    @Column(name = "address_district", length = 32)
    private String addressDistrict;

    @Column(name = "address_detail", length = 256)
    private String addressDetail;

    @Column(name = "created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<OrderItem> items = new ArrayList<>();

    @PreUpdate
    protected void onUpdate() { this.updatedAt = LocalDateTime.now(); }

    private String getFullAddress() {
        return Stream.of(addressProvince, addressCity, addressDistrict, addressDetail)
                .filter(Objects::nonNull).collect(Collectors.joining());
    }

    private String getStatusDisplay() {
        return switch (status) {
            case "pending" -> "待付款";
            case "paid" -> "已付款";
            case "shipped" -> "已发货";
            case "delivered" -> "已完成";
            case "cancelled" -> "已取消";
            default -> status;
        };
    }

    public Map<String, Object> toDict(boolean includeItems) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", id);
        map.put("user_id", user != null ? user.getId() : null);
        map.put("status", status);
        map.put("status_display", getStatusDisplay());
        map.put("total_amount", totalAmount);
        map.put("note", note);
        map.put("recipient_name", recipientName);
        map.put("recipient_phone", recipientPhone);
        map.put("address_province", addressProvince);
        map.put("address_city", addressCity);
        map.put("address_district", addressDistrict);
        map.put("address_detail", addressDetail);
        map.put("address_full", getFullAddress().isEmpty() ? null : getFullAddress());
        map.put("item_count", items.size());
        map.put("username", user != null ? user.getUsername() : null);
        map.put("created_at", createdAt != null ? createdAt.toString() : null);
        map.put("updated_at", updatedAt != null ? updatedAt.toString() : null);
        if (includeItems) {
            map.put("items", items.stream().map(OrderItem::toDict).collect(Collectors.toList()));
        }
        return map;
    }
}
