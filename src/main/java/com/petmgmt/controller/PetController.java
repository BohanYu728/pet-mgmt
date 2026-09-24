package com.petmgmt.controller;

import com.petmgmt.dto.ApiResponse;
import com.petmgmt.model.Pet;
import com.petmgmt.model.User;
import com.petmgmt.repository.PetRepository;
import com.petmgmt.repository.UserRepository;
import com.petmgmt.service.PetService;
import com.petmgmt.util.FileUtils;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

@RestController
@RequestMapping("/api/pets")
public class PetController {

    private final PetService petService;
    private final PetRepository petRepository;
    private final UserRepository userRepository;

    public PetController(PetService petService, PetRepository petRepository, UserRepository userRepository) {
        this.petService = petService;
        this.petRepository = petRepository;
        this.userRepository = userRepository;
    }

    private User getCurrentUser(Authentication auth) {
        if (auth == null) return null;
        return userRepository.findById((Long) auth.getPrincipal()).orElse(null);
    }

    @GetMapping
    public ResponseEntity<?> list(Authentication auth, @RequestParam(defaultValue = "1") int page) {
        User user = getCurrentUser(auth);
        if (user == null) return ResponseEntity.status(401).body(ApiResponse.error("请先登录"));
        var result = petRepository.findByOwnerId(user.getId(), PageRequest.of(page - 1, 9));
        return ResponseEntity.ok(Map.of(
            "pets", result.getContent().stream().map(Pet::toDict).toList(),
            "page", page, "pages", result.getTotalPages(), "total", result.getTotalElements()
        ));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> detail(Authentication auth, @PathVariable Long id) {
        User user = getCurrentUser(auth);
        Pet pet = petRepository.findById(id).orElse(null);
        if (pet == null) return ResponseEntity.notFound().build();
        if (!pet.getOwner().getId().equals(user.getId()) && !"admin".equals(user.getRole()))
            return ResponseEntity.status(403).body(ApiResponse.error("无权访问"));
        return ResponseEntity.ok(Map.of("pet", pet.toDict()));
    }

    @PostMapping
    public ResponseEntity<?> create(Authentication auth, @RequestBody Map<String, Object> data) {
        User user = getCurrentUser(auth);
        if (user == null) return ResponseEntity.status(401).body(ApiResponse.error("请先登录"));
        return ResponseEntity.ok(petService.createPet(user, data));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(Authentication auth, @PathVariable Long id, @RequestBody Map<String, Object> data) {
        User user = getCurrentUser(auth);
        if (user == null) return ResponseEntity.status(401).body(ApiResponse.error("请先登录"));
        Pet pet = petRepository.findById(id).orElse(null);
        if (pet == null) return ResponseEntity.notFound().build();
        if (!pet.getOwner().getId().equals(user.getId()))
            return ResponseEntity.status(403).body(ApiResponse.error("无权操作"));
        return ResponseEntity.ok(petService.updatePet(pet, data));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(Authentication auth, @PathVariable Long id) {
        User user = getCurrentUser(auth);
        if (user == null) return ResponseEntity.status(401).body(ApiResponse.error("请先登录"));
        Pet pet = petRepository.findById(id).orElse(null);
        if (pet == null) return ResponseEntity.notFound().build();
        if (!pet.getOwner().getId().equals(user.getId()))
            return ResponseEntity.status(403).body(ApiResponse.error("无权操作"));
        return ResponseEntity.ok(petService.deletePet(pet));
    }

    @PostMapping("/{id}/avatar")
    public ResponseEntity<?> uploadAvatar(Authentication auth, @PathVariable Long id, @RequestParam("avatar") MultipartFile file) {
        User user = getCurrentUser(auth);
        if (user == null) return ResponseEntity.status(401).body(ApiResponse.error("请先登录"));
        Pet pet = petRepository.findById(id).orElse(null);
        if (pet == null) return ResponseEntity.notFound().build();
        String path = FileUtils.saveUpload(file, "avatars");
        if (path == null) return ResponseEntity.badRequest().body(ApiResponse.error("不支持的图片格式"));
        if (pet.getAvatar() != null) FileUtils.deleteUpload(pet.getAvatar());
        pet.setAvatar(path);
        petRepository.save(pet);
        return ResponseEntity.ok(ApiResponse.success("上传成功", Map.of("avatar", "/" + path)));
    }
}
