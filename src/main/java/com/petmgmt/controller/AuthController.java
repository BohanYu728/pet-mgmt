package com.petmgmt.controller;

import com.petmgmt.dto.*;
import com.petmgmt.model.User;
import com.petmgmt.repository.UserRepository;
import com.petmgmt.security.JwtTokenProvider;
import com.petmgmt.security.TokenRevocationService;
import com.petmgmt.service.AuthService;
import com.petmgmt.util.FileUtils;
import jakarta.validation.Valid;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping({"/api/auth", "/auth"})
public class AuthController {

    private final AuthService authService;
    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final TokenRevocationService revocations;
    @Value("${app.cookie-secure:false}") private boolean cookieSecure;
    @Value("${jwt.expiration}") private long tokenLifetimeMs;

    public AuthController(AuthService authService, UserRepository userRepository, JwtTokenProvider jwtTokenProvider,
                          TokenRevocationService revocations) {
        this.authService = authService;
        this.userRepository = userRepository;
        this.jwtTokenProvider = jwtTokenProvider;
        this.revocations = revocations;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest req) {
        ApiResponse<Map<String, Object>> result = authService.login(req);
        if (result.getError() != null) return ResponseEntity.badRequest().body(result);
        return withToken(result);
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest req) {
        ApiResponse<Map<String, Object>> result = authService.register(req);
        if (result.getError() != null) return ResponseEntity.badRequest().body(result);
        return withToken(result);
    }

    @PostMapping("/merchant/register")
    public ResponseEntity<?> registerMerchant(@Valid @RequestBody RegisterRequest req) {
        ApiResponse<Map<String, Object>> result = authService.registerMerchant(req);
        if (result.getError() != null) return ResponseEntity.badRequest().body(result);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/csrf")
    public Map<String, String> csrf(HttpServletRequest request) {
        CsrfToken token=(CsrfToken) request.getAttribute(CsrfToken.class.getName());
        return Map.of("token", token.getToken(), "headerName", token.getHeaderName());
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request) {
        if (request.getCookies()!=null) for (Cookie cookie : request.getCookies())
            if ("token".equals(cookie.getName())) revocations.revoke(cookie.getValue());
        return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, cookie("", 0).toString())
                .body(ApiResponse.success("已退出"));
    }

    private ResponseEntity<?> withToken(ApiResponse<Map<String, Object>> result) {
        @SuppressWarnings("unchecked")
        Map<String, Object> user = (Map<String, Object>) result.getData().get("user");
        String token = jwtTokenProvider.generateToken(((Number) user.get("id")).longValue());
        return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE,
                cookie(token, tokenLifetimeMs / 1000).toString()).body(result);
    }

    private ResponseCookie cookie(String value, long maxAgeSeconds) {
        return ResponseCookie.from("token", value).httpOnly(true).secure(cookieSecure)
                .sameSite("Lax").path("/").maxAge(maxAgeSeconds).build();
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
