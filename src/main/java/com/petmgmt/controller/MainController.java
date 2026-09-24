package com.petmgmt.controller;

import com.petmgmt.model.*;
import com.petmgmt.repository.*;
import com.petmgmt.security.JwtTokenProvider;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.*;

@RestController
public class MainController {

    private final UserRepository userRepository;
    private final PetRepository petRepository;
    private final ReminderRepository reminderRepository;
    private final HealthRecordRepository healthRecordRepository;
    private final JwtTokenProvider jwtTokenProvider;

    public MainController(UserRepository ur, PetRepository pr, ReminderRepository rr,
                          HealthRecordRepository hr, JwtTokenProvider jp) {
        this.userRepository = ur; this.petRepository = pr; this.reminderRepository = rr;
        this.healthRecordRepository = hr; this.jwtTokenProvider = jp;
    }

    private User getCurrentUser(@CookieValue(value = "token", required = false) String token,
                                 @RequestHeader(value = "Authorization", required = false) String authHeader) {
        String t = token;
        if (t == null && authHeader != null && authHeader.startsWith("Bearer ")) {
            t = authHeader.substring(7);
        }
        if (t != null && jwtTokenProvider.validateToken(t)) {
            Long userId = jwtTokenProvider.getUserIdFromToken(t);
            return userRepository.findById(userId).orElse(null);
        }
        return null;
    }

    @GetMapping("/api/dashboard")
    public ResponseEntity<?> dashboard(
            @CookieValue(value = "token", required = false) String token,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        User user = getCurrentUser(token, authHeader);
        if (user == null) return ResponseEntity.status(401).body(Map.of("error", "请先登录"));

        List<Pet> pets = petRepository.findByOwnerId(user.getId());
        LocalDate today = LocalDate.now();
        List<Reminder> upcoming = reminderRepository.findByUserIdAndReminderDateBetweenAndIsDismissedFalse(
            user.getId(), today, today.plusDays(7));

        return ResponseEntity.ok(Map.of(
            "user", user.toDict(), "pets", pets.stream().map(Pet::toDict).toList(),
            "pet_count", pets.size(),
            "upcoming_reminders", upcoming.stream().map(Reminder::toDict).toList()
        ));
    }

    @GetMapping("/api/stats")
    public ResponseEntity<?> stats(
            @CookieValue(value = "token", required = false) String token,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        User user = getCurrentUser(token, authHeader);
        if (user == null) return ResponseEntity.status(401).body(Map.of("error", "请先登录"));

        return ResponseEntity.ok(Map.of(
            "total_records", healthRecordRepository.countByCreatorId(user.getId()),
            "ai_analyses", 0 // simplified
        ));
    }

    @GetMapping("/api/reminders/unread")
    public ResponseEntity<?> unreadReminders(
            @CookieValue(value = "token", required = false) String token,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        User user = getCurrentUser(token, authHeader);
        if (user == null) return ResponseEntity.ok(Map.of("reminders", List.of()));

        List<Reminder> reminders = reminderRepository
            .findByUserIdAndIsReadFalseAndIsDismissedFalseOrderByReminderDateAsc(user.getId())
            .stream().limit(5).toList();

        return ResponseEntity.ok(Map.of("reminders", reminders.stream().map(r -> Map.of(
            "id", r.getId(), "title", r.getTitle(),
            "reminder_date", r.getReminderDate().toString(),
            "pet_name", r.getPet() != null ? r.getPet().getName() : ""
        )).toList()));
    }

    @GetMapping("/api/search")
    public ResponseEntity<?> search(
            @CookieValue(value = "token", required = false) String token,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestParam(required = false, defaultValue = "") String q) {
        User user = getCurrentUser(token, authHeader);
        if (user == null) return ResponseEntity.status(401).body(Map.of("error", "请先登录"));
        if (q.isEmpty()) return ResponseEntity.ok(Map.of("pets", List.of(), "records", List.of()));

        List<Pet> pets = petRepository.findByOwnerIdAndNameContainingOrBreedContaining(user.getId(), q, q);
        return ResponseEntity.ok(Map.of(
            "pets", pets.stream().map(Pet::toDict).toList(),
            "records", List.of()
        ));
    }
}
