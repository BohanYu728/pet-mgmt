package com.petmgmt.controller;

import com.petmgmt.model.User;
import com.petmgmt.model.RentalRecord;
import com.petmgmt.repository.UserRepository;
import com.petmgmt.repository.RentalRecordRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/merchant")
public class MerchantBillingController {
    private final UserRepository users;
    private final RentalRecordRepository rentals;

    public MerchantBillingController(UserRepository users, RentalRecordRepository rentals) {
        this.users = users; this.rentals = rentals;
    }

    @GetMapping("/billing")
    public ResponseEntity<?> billing(Authentication auth) {
        if (auth == null || !(auth.getPrincipal() instanceof Long userId)) return ResponseEntity.status(401).body(Map.of("error", "请先登录"));
        User merchant = users.findById(userId).orElse(null);
        if (merchant == null || !"merchant".equals(merchant.getRole())) return ResponseEntity.status(403).body(Map.of("error", "需要商户权限"));
        return ResponseEntity.ok(Map.of("merchant", merchant.toDict(), "rental_records",
            rentals.findByMerchantIdOrderByCreatedAtDesc(userId).stream().map(RentalRecord::toDict).toList()));
    }
}
