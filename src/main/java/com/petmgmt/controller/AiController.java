package com.petmgmt.controller;

import com.petmgmt.dto.ApiResponse;
import com.petmgmt.model.*;
import com.petmgmt.repository.*;
import com.petmgmt.service.AiService;
import com.petmgmt.util.FileUtils;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

@RestController
@RequestMapping("/api/ai")
public class AiController {

    private final AiService aiService;
    private final UserRepository userRepository;
    private final PetRepository petRepository;
    private final BreedHistoryRepository breedHistoryRepository;
    private final EmotionHistoryRepository emotionHistoryRepository;

    public AiController(AiService aiService, UserRepository userRepository, PetRepository petRepository,
                        BreedHistoryRepository breedHistoryRepository, EmotionHistoryRepository emotionHistoryRepository) {
        this.aiService = aiService;
        this.userRepository = userRepository;
        this.petRepository = petRepository;
        this.breedHistoryRepository = breedHistoryRepository;
        this.emotionHistoryRepository = emotionHistoryRepository;
    }

    private User getUser(Authentication auth) {
        if (auth == null) return null;
        return userRepository.findById((Long) auth.getPrincipal()).orElse(null);
    }

    @PostMapping("/breed/recognize")
    public ResponseEntity<?> recognizeBreed(Authentication auth,
            @RequestParam("image") MultipartFile file, @RequestParam(required = false) Long petId) {
        User user = getUser(auth);
        if (user == null) return ResponseEntity.status(401).body(ApiResponse.error("请先登录"));

        Pet pet = petId != null ? petRepository.findById(petId).orElse(null) : null;
        if (petId != null && (pet == null || !user.getId().equals(pet.getOwner().getId())))
            return ResponseEntity.status(403).body(ApiResponse.error("无权操作此宠物"));

        String imagePath = FileUtils.saveUpload(file, "ai");
        if (imagePath == null) return ResponseEntity.badRequest().body(ApiResponse.error("不支持的图片格式"));

        String fullPath = "src/main/resources/static/" + imagePath;
        Map<String, Object> result;
        try { result = aiService.predictBreed(fullPath); }
        catch (IllegalArgumentException ex) {
            FileUtils.deleteUpload(imagePath);
            return ResponseEntity.badRequest().body(ApiResponse.error(ex.getMessage()));
        }
        catch (IllegalStateException ex) {
            FileUtils.deleteUpload(imagePath);
            return ResponseEntity.status(503).body(ApiResponse.error("图片识别服务暂不可用，请稍后重试"));
        }

        BreedHistory bh = aiService.saveBreedHistory(user, pet, imagePath, result);
        result.put("history_id", bh.getId());
        result.put("image_url", "/" + imagePath);

        return ResponseEntity.ok(result);
    }

    @GetMapping("/breed/history")
    public ResponseEntity<?> breedHistory(Authentication auth, @RequestParam(defaultValue = "1") int page) {
        User user = getUser(auth);
        if (user == null) return ResponseEntity.status(401).body(ApiResponse.error("请先登录"));
        var result = breedHistoryRepository.findByUserIdOrderByCreatedAtDesc(user.getId(), PageRequest.of(page - 1, 12));
        return ResponseEntity.ok(Map.of("items", result.getContent().stream().map(BreedHistory::toDict).toList(),
            "page", page, "pages", result.getTotalPages()));
    }

    @PostMapping("/emotion/recognize")
    public ResponseEntity<?> recognizeEmotion(Authentication auth,
            @RequestParam("image") MultipartFile file, @RequestParam Long petId) {
        User user = getUser(auth);
        if (user == null) return ResponseEntity.status(401).body(ApiResponse.error("请先登录"));
        Pet pet = petRepository.findById(petId).orElse(null);
        if (pet == null || !pet.getOwner().getId().equals(user.getId()))
            return ResponseEntity.status(403).body(ApiResponse.error("无权操作"));

        String imagePath = FileUtils.saveUpload(file, "ai");
        if (imagePath == null) return ResponseEntity.badRequest().body(ApiResponse.error("不支持的图片格式"));

        String fullPath = "src/main/resources/static/" + imagePath;
        Map<String, Object> result;
        try { result = aiService.predictEmotion(fullPath); }
        catch (IllegalArgumentException ex) {
            FileUtils.deleteUpload(imagePath);
            return ResponseEntity.badRequest().body(ApiResponse.error(ex.getMessage()));
        }
        catch (IllegalStateException ex) {
            FileUtils.deleteUpload(imagePath);
            return ResponseEntity.status(503).body(ApiResponse.error("图片识别服务暂不可用，请稍后重试"));
        }
        EmotionHistory eh = aiService.saveEmotionHistory(user, pet, imagePath, result);
        result.put("history_id", eh.getId());
        result.put("image_url", "/" + imagePath);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/emotion/data/{petId}")
    public ResponseEntity<?> emotionData(Authentication auth, @PathVariable Long petId) {
        User user = getUser(auth);
        if (user == null) return ResponseEntity.status(401).body(ApiResponse.error("请先登录"));
        Pet pet = petRepository.findById(petId).orElse(null);
        if (pet == null || !pet.getOwner().getId().equals(user.getId()))
            return ResponseEntity.status(403).body(ApiResponse.error("无权访问"));
        List<EmotionHistory> history = emotionHistoryRepository.findTop10ByPetIdOrderByCreatedAtDesc(petId);
        return ResponseEntity.ok(Map.of("emotions", history.stream().map(EmotionHistory::toDict).toList()));
    }

    @PostMapping("/health-report/{petId}")
    public ResponseEntity<?> healthReport(Authentication auth, @PathVariable Long petId) {
        User user = getUser(auth);
        if (user == null) return ResponseEntity.status(401).body(ApiResponse.error("请先登录"));
        Pet pet = petRepository.findById(petId).orElse(null);
        if (pet == null || !pet.getOwner().getId().equals(user.getId()))
            return ResponseEntity.status(403).body(ApiResponse.error("无权操作"));
        String report = aiService.generateHealthReport(pet);
        return ResponseEntity.ok(Map.of("report", report));
    }

    @PostMapping("/recommend/{petId}")
    public ResponseEntity<?> recommend(Authentication auth, @PathVariable Long petId) {
        User user = getUser(auth);
        if (user == null) return ResponseEntity.status(401).body(ApiResponse.error("请先登录"));
        Pet pet = petRepository.findById(petId).orElse(null);
        if (pet == null || !pet.getOwner().getId().equals(user.getId()))
            return ResponseEntity.status(403).body(ApiResponse.error("无权操作"));
        String recommendation = aiService.recommendProducts(pet);
        return ResponseEntity.ok(Map.of("recommendation", recommendation));
    }
}
