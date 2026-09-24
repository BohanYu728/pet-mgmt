package com.petmgmt.controller;

import com.petmgmt.dto.*;
import com.petmgmt.model.User;
import com.petmgmt.repository.UserRepository;
import com.petmgmt.security.JwtTokenProvider;
import com.petmgmt.service.AuthService;
import com.petmgmt.util.FileUtils;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;
    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthController(AuthService authService, UserRepository userRepository, JwtTokenProvider jwtTokenProvider) {
        this.authService = authService;
        this.userRepository = userRepository;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest req) {
        ApiResponse<Map<String, Object>> result = authService.login(req);
        if (result.getError() != null) return ResponseEntity.badRequest().body(result);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest req) {
        ApiResponse<Map<String, Object>> result = authService.register(req);
        if (result.getError() != null) return ResponseEntity.badRequest().body(result);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/merchant/register")
    public ResponseEntity<?> registerMerchant(@Valid @RequestBody RegisterRequest req) {
        ApiResponse<Map<String, Object>> result = authService.registerMerchant(req);
        if (result.getError() != null) return ResponseEntity.badRequest().body(result);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/profile")
    public ResponseEntity<?> getProfile(Authentication auth) {
        if (auth == null) return ResponseEntity.status(401).body(ApiResponse.error("请先登录"));
        Long userId = (Long) auth.getPrincipal();
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) return ResponseEntity.status(401).body(ApiResponse.error("用户不存在"));
        return ResponseEntity.ok(Map.of("user", user.toDict()));
    }

    @PutMapping("/profile")
    public ResponseEntity<?> updateProfile(Authentication auth, @RequestBody Map<String, Object> data) {
        if (auth == null) return ResponseEntity.status(401).body(ApiResponse.error("请先登录"));
        Long userId = (Long) auth.getPrincipal();
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) return ResponseEntity.status(401).body(ApiResponse.error("用户不存在"));
        return ResponseEntity.ok(authService.updateProfile(user,
            (String) data.get("username"), (String) data.get("email"), (String) data.get("phone")));
    }

    @PutMapping("/password")
    public ResponseEntity<?> changePassword(Authentication auth, @RequestBody Map<String, String> data) {
        if (auth == null) return ResponseEntity.status(401).body(ApiResponse.error("请先登录"));
        Long userId = (Long) auth.getPrincipal();
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) return ResponseEntity.status(401).body(ApiResponse.error("用户不存在"));
        return ResponseEntity.ok(authService.changePassword(user,
            data.get("old_password"), data.get("new_password")));
    }

    @PostMapping("/avatar")
    public ResponseEntity<?> uploadAvatar(Authentication auth, @RequestParam("avatar") MultipartFile file) {
        if (auth == null) return ResponseEntity.status(401).body(ApiResponse.error("请先登录"));
        Long userId = (Long) auth.getPrincipal();
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) return ResponseEntity.status(401).body(ApiResponse.error("用户不存在"));
        String path = FileUtils.saveUpload(file, "avatars");
        if (path == null) return ResponseEntity.badRequest().body(ApiResponse.error("不支持的图片格式"));
        if (user.getAvatar() != null) FileUtils.deleteUpload(user.getAvatar());
        user.setAvatar(path);
        userRepository.save(user);
        return ResponseEntity.ok(ApiResponse.success("上传成功", Map.of("avatar", "/" + path)));
    }
}
