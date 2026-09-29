package com.petmgmt.controller;

import com.petmgmt.dto.ApiResponse;
import com.petmgmt.model.*;
import com.petmgmt.repository.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.*;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final UserRepository userRepository;
    private final PetRepository petRepository;
    private final HealthRecordRepository healthRecordRepository;
    private final BreedHistoryRepository breedHistoryRepository;
    private final EmotionHistoryRepository emotionHistoryRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;

    public AdminController(UserRepository ur, PetRepository pr, HealthRecordRepository hr,
                           BreedHistoryRepository bh, EmotionHistoryRepository eh,
                           ProductRepository prodR, OrderRepository or) {
        this.userRepository = ur; this.petRepository = pr; this.healthRecordRepository = hr;
        this.breedHistoryRepository = bh; this.emotionHistoryRepository = eh;
        this.productRepository = prodR; this.orderRepository = or;
    }

    private boolean isAdmin(Authentication auth) {
        if (auth == null) return false;
        User u = userRepository.findById((Long) auth.getPrincipal()).orElse(null);
        return u != null && "admin".equals(u.getRole());
    }

    @GetMapping("/stats")
    public ResponseEntity<?> stats(Authentication auth) {
        if (!isAdmin(auth)) return ResponseEntity.status(403).body(ApiResponse.error("需要管理员权限"));
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("user_count", userRepository.countByRole("user"));
        stats.put("pet_count", petRepository.count());
        stats.put("health_record_count", healthRecordRepository.count());
        stats.put("ai_analyses", breedHistoryRepository.count() + emotionHistoryRepository.count());
        stats.put("merchant_count", userRepository.countByRole("merchant"));
        stats.put("product_count", productRepository.countByIsActiveTrue());
        stats.put("order_count", orderRepository.count());
        stats.put("dog_count", petRepository.countBySpecies("dog"));
        stats.put("cat_count", petRepository.countBySpecies("cat"));
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/users/list")
    public ResponseEntity<?> usersList(Authentication auth) {
        if (!isAdmin(auth)) return ResponseEntity.status(403).body(ApiResponse.error("需要管理员权限"));
        List<User> users = userRepository.findByRole("user");
        List<Map<String, Object>> result = new ArrayList<>();
        for (User u : users) {
            Map<String, Object> m = u.toDict();
            m.put("pet_count", petRepository.countByOwnerId(u.getId()));
            result.add(m);
        }
        return ResponseEntity.ok(Map.of("users", result));
    }

    @GetMapping("/pets/list")
    public ResponseEntity<?> petsList(Authentication auth) {
        if (!isAdmin(auth)) return ResponseEntity.status(403).body(ApiResponse.error("需要管理员权限"));
        List<Map<String, Object>> result = new ArrayList<>();
        for (Pet p : petRepository.findAll()) {
            Map<String, Object> m = p.toDict();
            m.put("owner_name", p.getOwner() != null ? p.getOwner().getUsername() : null);
            result.add(m);
        }
        return ResponseEntity.ok(Map.of("pets", result));
    }

    @DeleteMapping("/pets/{id}")
    public ResponseEntity<?> deletePet(Authentication auth, @PathVariable Long id) {
        if (!isAdmin(auth)) return ResponseEntity.status(403).body(ApiResponse.error("需要管理员权限"));
        petRepository.deleteById(id);
        return ResponseEntity.ok(ApiResponse.success("删除成功"));
    }

    @GetMapping("/merchants/list")
    public ResponseEntity<?> merchantsList(Authentication auth) {
        if (!isAdmin(auth)) return ResponseEntity.status(403).body(ApiResponse.error("需要管理员权限"));
        List<User> merchants = userRepository.findByRole("merchant");
        return ResponseEntity.ok(Map.of("merchants", merchants.stream().map(User::toDict).toList()));
    }

    @PostMapping("/merchants/{id}/approve")
    public ResponseEntity<?> approveMerchant(Authentication auth, @PathVariable Long id) {
        if (!isAdmin(auth)) return ResponseEntity.status(403).body(ApiResponse.error("需要管理员权限"));
        User m = userRepository.findById(id).orElse(null);
        if (m == null) return ResponseEntity.notFound().build();
        m.setIsApproved(true);
        userRepository.save(m);
        return ResponseEntity.ok(ApiResponse.success("已审核通过"));
    }
}
