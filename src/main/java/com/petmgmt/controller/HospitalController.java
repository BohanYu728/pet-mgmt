package com.petmgmt.controller;

import com.petmgmt.model.Hospital;
import com.petmgmt.model.Review;
import com.petmgmt.model.User;
import com.petmgmt.repository.HospitalRepository;
import com.petmgmt.repository.ReviewRepository;
import com.petmgmt.repository.UserRepository;
import com.petmgmt.service.HospitalService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/hospitals")
public class HospitalController {

    private final HospitalService hospitalService;
    private final HospitalRepository hospitalRepository;
    private final ReviewRepository reviewRepository;
    private final UserRepository userRepository;

    public HospitalController(HospitalService hs, HospitalRepository hr, ReviewRepository rr, UserRepository ur) {
        this.hospitalService = hs; this.hospitalRepository = hr; this.reviewRepository = rr; this.userRepository = ur;
    }

    @GetMapping({"", "/api"})
    public ResponseEntity<?> apiHospitals() {
        return ResponseEntity.ok(Map.of("hospitals", hospitalService.getAllHospitals()));
    }

    @GetMapping("/nearby")
    public ResponseEntity<?> nearby(@RequestParam double lat, @RequestParam double lng,
                                     @RequestParam(defaultValue = "10") double radius) {
        return ResponseEntity.ok(Map.of("hospitals", hospitalService.getNearbyHospitals(lat, lng, radius)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> detail(@PathVariable Long id) {
        Hospital h = hospitalRepository.findById(id).orElse(null);
        if (h == null) return ResponseEntity.notFound().build();
        List<Review> reviews = reviewRepository.findByHospitalIdOrderByCreatedAtDesc(id);
        return ResponseEntity.ok(Map.of("hospital", h.toDict(),
            "reviews", reviews.stream().map(Review::toDict).toList()));
    }

    @PostMapping("/{id}/reviews")
    public ResponseEntity<?> addReview(Authentication auth, @PathVariable Long id, @RequestBody Map<String, Object> data) {
        if (auth == null) return ResponseEntity.status(401).body(Map.of("error", "请先登录"));
        User user = userRepository.findById((Long) auth.getPrincipal()).orElse(null);
        if (user == null) return ResponseEntity.status(401).body(Map.of("error", "请先登录"));
        if (!(data.get("rating") instanceof Number rating) || !Double.isFinite(rating.doubleValue()) ||
                rating.doubleValue() != rating.intValue() || rating.intValue() < 1 || rating.intValue() > 5)
            return ResponseEntity.badRequest().body(Map.of("error", "评分必须为1至5的整数"));
        if (data.containsKey("content") && (!(data.get("content") instanceof String content) || content.length() > 2000))
            return ResponseEntity.badRequest().body(Map.of("error", "评价须为不超过2000字的文本"));
        Hospital h = hospitalRepository.findById(id).orElse(null);
        if (h == null) return ResponseEntity.notFound().build();

        Review r = new Review();
        r.setUser(user); r.setHospital(h);
        r.setRating(((Number) data.get("rating")).intValue());
        r.setContent((String) data.getOrDefault("content", ""));
        reviewRepository.save(r);
        return ResponseEntity.ok(Map.of("review", r.toDict()));
    }
}
