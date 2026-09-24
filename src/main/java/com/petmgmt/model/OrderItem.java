package com.petmgmt.model;

import jakarta.persistence.*;
import lombok.*;
import java.util.*;

@Entity
@Table(name = "order_items")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;

    @Column(name = "product_name", nullable = false, length = 128)
    private String productName;

    @Column(name = "product_price", nullable = false)
    private Double productPrice;

    @Column(nullable = false)
    private Integer quantity = 1;

    @Column(nullable = false)
    private Double subtotal;

    public Map<String, Object> toDict() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", id);
        map.put("order_id", order != null ? order.getId() : null);
        map.put("product_id", product != null ? product.getId() : null);
        map.put("product_name", productName);
        map.put("product_price", productPrice);
        map.put("quantity", quantity);
        map.put("subtotal", subtotal);
        return map;
    }
}
