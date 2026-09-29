package com.petmgmt.controller;

import com.petmgmt.model.*;
import com.petmgmt.repository.*;
import com.petmgmt.service.HealthService;
import com.petmgmt.util.FileUtils;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.*;

/** REST operations used by the Vue pet journal. Every pet lookup is scoped to its owner. */
@RestController
@RequestMapping("/api/pets/{petId}")
public class PetJournalController {
    private final PetRepository pets;
    private final HealthRecordRepository health;
    private final ReminderRepository reminders;
    private final WeightRecordRepository weights;
    private final DailyLogRepository logs;
    private final PetPhotoRepository photos;
    private final HealthService healthService;

    public PetJournalController(PetRepository pets, HealthRecordRepository health,
            ReminderRepository reminders, WeightRecordRepository weights,
            DailyLogRepository logs, PetPhotoRepository photos, HealthService healthService) {
        this.pets = pets; this.health = health; this.reminders = reminders;
        this.weights = weights; this.logs = logs; this.photos = photos; this.healthService = healthService;
    }

    private Pet owned(Long petId, Authentication auth) {
        if (auth == null || !(auth.getPrincipal() instanceof Long userId)) return null;
        return pets.findById(petId).filter(p -> p.getOwner() != null && userId.equals(p.getOwner().getId())).orElse(null);
    }

    private ResponseEntity<?> denied() { return ResponseEntity.status(403).body(Map.of("error", "无权访问此宠物")); }
    private String string(Map<String, Object> data, String key) { return Objects.toString(data.get(key), "").trim(); }
    private LocalDate date(Map<String, Object> data, String key) {
        String value = string(data, key);
        try { return value.isEmpty() ? LocalDate.now() : LocalDate.parse(value); }
        catch (java.time.format.DateTimeParseException ex) { throw new IllegalArgumentException("日期格式应为 YYYY-MM-DD"); }
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<?> invalidInput(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage() == null ? "输入内容有误" : ex.getMessage()));
    }

    @GetMapping("/health")
    public ResponseEntity<?> healthList(@PathVariable Long petId, Authentication auth) {
        if (owned(petId, auth) == null) return denied();
        return ResponseEntity.ok(Map.of("items", health.findByPetIdOrderByRecordDateDesc(petId).stream().map(HealthRecord::toDict).toList()));
    }

    @PostMapping("/health")
    public ResponseEntity<?> addHealth(@PathVariable Long petId, Authentication auth, @RequestBody Map<String, Object> data) {
        Pet pet = owned(petId, auth);
        if (pet == null) return denied();
        if (string(data, "title").isEmpty() || string(data, "record_type").isEmpty()) return ResponseEntity.badRequest().body(Map.of("error", "请填写标题和记录类型"));
        if (!string(data, "record_date").isEmpty()) date(data, "record_date");
        if (!string(data, "next_due_date").isEmpty()) date(data, "next_due_date");
        return ResponseEntity.ok(healthService.createHealthRecord(pet.getOwner(), pet, data));
    }

    @PutMapping("/health/{id}")
    public ResponseEntity<?> updateHealth(@PathVariable Long petId, @PathVariable Long id, Authentication auth, @RequestBody Map<String, Object> data) {
        if (owned(petId, auth) == null) return denied();
        HealthRecord record = health.findById(id).orElse(null);
        if (record == null || !petId.equals(record.getPet().getId())) return ResponseEntity.notFound().build();
        if (!string(data, "record_date").isEmpty()) date(data, "record_date");
        if (!string(data, "next_due_date").isEmpty()) date(data, "next_due_date");
        return ResponseEntity.ok(healthService.updateHealthRecord(record, data));
    }

    @DeleteMapping("/health/{id}")
    public ResponseEntity<?> deleteHealth(@PathVariable Long petId, @PathVariable Long id, Authentication auth) {
        if (owned(petId, auth) == null) return denied();
        HealthRecord record = health.findById(id).orElse(null);
        if (record == null || !petId.equals(record.getPet().getId())) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(healthService.deleteHealthRecord(record));
    }

    @GetMapping("/reminders")
    public ResponseEntity<?> reminderList(@PathVariable Long petId, Authentication auth) {
        if (owned(petId, auth) == null) return denied();
        return ResponseEntity.ok(Map.of("items", reminders.findByPetId(petId).stream().map(Reminder::toDict).toList()));
    }

    @PostMapping("/reminders")
    public ResponseEntity<?> addReminder(@PathVariable Long petId, Authentication auth, @RequestBody Map<String, Object> data) {
        Pet pet = owned(petId, auth);
        if (pet == null) return denied();
        if (string(data, "title").isEmpty() || string(data, "reminder_date").isEmpty()) return ResponseEntity.badRequest().body(Map.of("error", "请填写标题和提醒日期"));
        Reminder item = new Reminder();
        item.setPet(pet); item.setUser(pet.getOwner()); item.setTitle(string(data, "title"));
        item.setReminderType(string(data, "reminder_type").isEmpty() ? "other" : string(data, "reminder_type"));
        item.setReminderDate(date(data, "reminder_date"));
        reminders.save(item);
        return ResponseEntity.ok(Map.of("item", item.toDict()));
    }

    @PutMapping("/reminders/{id}")
    public ResponseEntity<?> updateReminder(@PathVariable Long petId, @PathVariable Long id, Authentication auth, @RequestBody Map<String, Object> data) {
        if (owned(petId, auth) == null) return denied();
        Reminder item = reminders.findById(id).orElse(null);
        if (item == null || !petId.equals(item.getPet().getId())) return ResponseEntity.notFound().build();
        if (data.containsKey("title")) item.setTitle(string(data, "title"));
        if (data.containsKey("reminder_type")) item.setReminderType(string(data, "reminder_type"));
        if (data.containsKey("reminder_date")) item.setReminderDate(date(data, "reminder_date"));
        if (data.containsKey("is_read")) item.setIsRead(Boolean.TRUE.equals(data.get("is_read")));
        if (data.containsKey("is_dismissed")) item.setIsDismissed(Boolean.TRUE.equals(data.get("is_dismissed")));
        reminders.save(item);
        return ResponseEntity.ok(Map.of("item", item.toDict()));
    }

    @DeleteMapping("/reminders/{id}")
    public ResponseEntity<?> deleteReminder(@PathVariable Long petId, @PathVariable Long id, Authentication auth) {
        if (owned(petId, auth) == null) return denied();
        Reminder item = reminders.findById(id).orElse(null);
        if (item == null || !petId.equals(item.getPet().getId())) return ResponseEntity.notFound().build();
        reminders.delete(item);
        return ResponseEntity.ok(Map.of("message", "已删除"));
    }

    @GetMapping("/weights")
    public ResponseEntity<?> weightList(@PathVariable Long petId, Authentication auth) {
        if (owned(petId, auth) == null) return denied();
        return ResponseEntity.ok(Map.of("items", weights.findByPetIdOrderByRecordDateDesc(petId).stream().map(WeightRecord::toDict).toList()));
    }

    @PostMapping("/weights")
    public ResponseEntity<?> addWeight(@PathVariable Long petId, Authentication auth, @RequestBody Map<String, Object> data) {
        Pet pet = owned(petId, auth);
        if (pet == null) return denied();
        if (!(data.get("weight") instanceof Number number) || number.doubleValue() <= 0) return ResponseEntity.badRequest().body(Map.of("error", "体重必须大于零"));
        WeightRecord item = new WeightRecord();
        item.setPet(pet); item.setWeight(number.doubleValue()); item.setRecordDate(date(data, "record_date")); item.setNotes(string(data, "notes"));
        weights.save(item);
        pet.setWeight(java.math.BigDecimal.valueOf(number.doubleValue())); pets.save(pet);
        return ResponseEntity.ok(Map.of("item", item.toDict()));
    }

    @DeleteMapping("/weights/{id}")
    public ResponseEntity<?> deleteWeight(@PathVariable Long petId, @PathVariable Long id, Authentication auth) {
        if (owned(petId, auth) == null) return denied();
        WeightRecord item = weights.findById(id).orElse(null);
        if (item == null || !petId.equals(item.getPet().getId())) return ResponseEntity.notFound().build();
        weights.delete(item); return ResponseEntity.ok(Map.of("message", "已删除"));
    }

    @GetMapping("/logs")
    public ResponseEntity<?> logList(@PathVariable Long petId, Authentication auth, @RequestParam(defaultValue = "1") int page) {
        if (owned(petId, auth) == null) return denied();
        var result = logs.findByPetIdOrderByLogDateDesc(petId, PageRequest.of(Math.max(0, page - 1), 30));
        return ResponseEntity.ok(Map.of("items", result.getContent(), "page", page, "pages", result.getTotalPages()));
    }

    @PostMapping("/logs")
    public ResponseEntity<?> addLog(@PathVariable Long petId, Authentication auth, @RequestBody Map<String, Object> data) {
        Pet pet = owned(petId, auth);
        if (pet == null) return denied();
        if (string(data, "title").isEmpty()) return ResponseEntity.badRequest().body(Map.of("error", "请填写日志标题"));
        DailyLog item = new DailyLog();
        item.setPetId(petId); item.setUserId(pet.getOwner().getId()); item.setTitle(string(data, "title"));
        item.setLogType(string(data, "log_type").isEmpty() ? "daily" : string(data, "log_type"));
        item.setLogDate(date(data, "log_date")); item.setContent(string(data, "content")); item.setNotes(string(data, "notes"));
        logs.save(item); return ResponseEntity.ok(Map.of("item", item));
    }

    @PutMapping("/logs/{id}")
    public ResponseEntity<?> updateLog(@PathVariable Long petId, @PathVariable Long id, Authentication auth, @RequestBody Map<String, Object> data) {
        if (owned(petId, auth) == null) return denied();
        DailyLog item = logs.findById(id).orElse(null);
        if (item == null || !petId.equals(item.getPetId())) return ResponseEntity.notFound().build();
        if (data.containsKey("title")) item.setTitle(string(data, "title"));
        if (data.containsKey("log_type")) item.setLogType(string(data, "log_type"));
        if (data.containsKey("log_date")) item.setLogDate(date(data, "log_date"));
        if (data.containsKey("content")) item.setContent(string(data, "content"));
        if (data.containsKey("notes")) item.setNotes(string(data, "notes"));
        logs.save(item); return ResponseEntity.ok(Map.of("item", item));
    }

    @DeleteMapping("/logs/{id}")
    public ResponseEntity<?> deleteLog(@PathVariable Long petId, @PathVariable Long id, Authentication auth) {
        if (owned(petId, auth) == null) return denied();
        DailyLog item = logs.findById(id).orElse(null);
        if (item == null || !petId.equals(item.getPetId())) return ResponseEntity.notFound().build();
        logs.delete(item); return ResponseEntity.ok(Map.of("message", "已删除"));
    }

    @GetMapping("/photos")
    public ResponseEntity<?> photoList(@PathVariable Long petId, Authentication auth) {
        if (owned(petId, auth) == null) return denied();
        return ResponseEntity.ok(Map.of("items", photos.findByPetIdOrderByCreatedAtDesc(petId).stream().map(this::photoDto).toList()));
    }

    private Map<String, Object> photoDto(PetPhoto item) {
        Map<String, Object> dto = new LinkedHashMap<>();
        dto.put("id", item.getId()); dto.put("pet_id", item.getPet().getId());
        dto.put("image_url", "/" + item.getImagePath()); dto.put("caption", item.getCaption());
        dto.put("is_primary", item.getIsPrimary());
        return dto;
    }

    @PostMapping("/photos")
    public ResponseEntity<?> addPhoto(@PathVariable Long petId, Authentication auth, @RequestParam("image") MultipartFile image,
            @RequestParam(required = false, defaultValue = "") String caption) {
        Pet pet = owned(petId, auth);
        if (pet == null) return denied();
        String path = FileUtils.saveUpload(image, "photos");
        if (path == null) return ResponseEntity.badRequest().body(Map.of("error", "不支持的图片格式"));
        PetPhoto item = new PetPhoto(); item.setPet(pet); item.setFileName(image.getOriginalFilename());
        item.setFilePath(path); item.setImagePath(path); item.setFileSize(image.getSize());
        item.setMimeType(image.getContentType()); item.setCaption(caption); item.setIsPrimary(false); item.setSortOrder(0);
        photos.save(item); return ResponseEntity.ok(Map.of("item", photoDto(item)));
    }

    @DeleteMapping("/photos/{id}")
    public ResponseEntity<?> deletePhoto(@PathVariable Long petId, @PathVariable Long id, Authentication auth) {
        if (owned(petId, auth) == null) return denied();
        PetPhoto item = photos.findById(id).orElse(null);
        if (item == null || !petId.equals(item.getPet().getId())) return ResponseEntity.notFound().build();
        photos.delete(item); FileUtils.deleteUpload(item.getFilePath());
        return ResponseEntity.ok(Map.of("message", "已删除"));
    }
}
