package com.petmgmt.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Entity
@Table(name = "rental_records")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RentalRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "merchant_id", nullable = false)
    private User merchant;

    @Column(nullable = false)
    private Double amount;

    @Column(name = "period_start")
    private LocalDate periodStart;

    @Column(name = "period_end")
    private LocalDate periodEnd;

    @Column(name = "paid_at")
    @Builder.Default
    private LocalDateTime paidAt = LocalDateTime.now();

    @Column(name = "collected_by")
    private Long collectedBy;

    @Column(name = "payment_method", length = 20)
    @Builder.Default
    private String paymentMethod = "bank";

    @Column(name = "transaction_id", length = 64)
    private String transactionId;

    @Column(columnDefinition = "TEXT")
    private String note;

    @Column(name = "created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    public Map<String, Object> toDict() {
        String methodDisplay = switch (paymentMethod) {
            case "alipay" -> "支付宝";
            case "wechat" -> "微信支付";
            case "bank" -> "银行转账";
            default -> paymentMethod;
        };
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", id);
        map.put("merchant_id", merchant != null ? merchant.getId() : null);
        map.put("merchant_name", merchant != null ? merchant.getStoreName() : null);
        map.put("amount", amount);
        map.put("period_start", periodStart != null ? periodStart.toString() : null);
        map.put("period_end", periodEnd != null ? periodEnd.toString() : null);
        map.put("paid_at", paidAt != null ? paidAt.toString() : null);
        map.put("collected_by", collectedBy);
        map.put("payment_method", paymentMethod);
        map.put("payment_method_display", methodDisplay);
        map.put("transaction_id", transactionId);
        map.put("note", note);
        map.put("created_at", createdAt != null ? createdAt.toString() : null);
        return map;
    }
}
