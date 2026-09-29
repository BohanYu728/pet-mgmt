package com.petmgmt.service;

import com.petmgmt.dto.ApiResponse;
import com.petmgmt.dto.LoginRequest;
import com.petmgmt.dto.RegisterRequest;
import com.petmgmt.model.User;
import com.petmgmt.repository.UserRepository;
import com.petmgmt.security.RoleAssignmentService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Map;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RoleAssignmentService roles;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, RoleAssignmentService roles) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.roles = roles;
    }

    @Transactional
    public ApiResponse<Map<String, Object>> register(RegisterRequest req) {
        if (userRepository.existsByUsername(req.getUsername())) {
            return ApiResponse.error("用户名已存在");
        }
        if (userRepository.existsByEmail(req.getEmail())) {
            return ApiResponse.error("邮箱已被注册");
        }
        User user = new User();
        user.setUsername(req.getUsername());
        user.setEmail(req.getEmail());
        user.setPasswordHash(passwordEncoder.encode(req.getPassword()));
        user.setPhone(req.getPhone());
        user.setRole("user");
        user.setIsApproved(true);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
        roles.assign(user.getId(), "user");

        return ApiResponse.success("注册成功", Map.of("user", user.toDict()));
    }

    @Transactional
    public ApiResponse<Map<String, Object>> registerMerchant(RegisterRequest req) {
        if (!StringUtils.hasText(req.getStoreName())) {
            return ApiResponse.error("店铺名称不能为空");
        }
        if (userRepository.existsByUsername(req.getUsername())) {
            return ApiResponse.error("用户名已存在");
        }
        if (userRepository.existsByEmail(req.getEmail())) {
            return ApiResponse.error("邮箱已被注册");
        }
        User user = new User();
        user.setUsername(req.getUsername());
        user.setEmail(req.getEmail());
        user.setPasswordHash(passwordEncoder.encode(req.getPassword()));
        user.setPhone(req.getPhone());
        user.setRole("merchant");
        user.setStoreName(req.getStoreName());
        user.setStorePhone(req.getPhone());
        user.setIsApproved(false);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
        roles.assign(user.getId(), "merchant");

        return ApiResponse.success("商户入驻申请已提交，请等待管理员审核", Map.of("user", user.toDict()));
    }

    public ApiResponse<Map<String, Object>> login(LoginRequest req) {
        User user = userRepository.findByUsername(req.getUsername()).orElse(null);
        if (user == null || !passwordEncoder.matches(req.getPassword(), user.getPasswordHash())) {
            return ApiResponse.error("用户名或密码错误");
        }

        String expectRole = req.getRole();
        if ("admin".equals(expectRole) && !"admin".equals(user.getRole())) {
            return ApiResponse.error("该账号不是管理员");
        }
        if ("merchant".equals(expectRole) && !"merchant".equals(user.getRole())) {
            return ApiResponse.error("该账号不是商户账号");
        }
        if ("merchant".equals(user.getRole()) && !Boolean.TRUE.equals(user.getIsApproved())) {
            return ApiResponse.error("您的商户账号正在审核中，请等待管理员审批");
        }

        return ApiResponse.success("登录成功", Map.of("user", user.toDict()));
    }

    @Transactional
    public ApiResponse<Map<String, Object>> updateProfile(User user, String username, String email, String phone) {
        if (StringUtils.hasText(username) && !username.equals(user.getUsername())) {
            if (userRepository.existsByUsername(username)) return ApiResponse.error("用户名已存在");
            user.setUsername(username);
        }
        if (StringUtils.hasText(email) && !email.equals(user.getEmail())) {
            if (userRepository.existsByEmail(email)) return ApiResponse.error("邮箱已被注册");
            user.setEmail(email);
        }
        if (phone != null) user.setPhone(phone);
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
        return ApiResponse.success("更新成功", Map.of("user", user.toDict()));
    }

    @Transactional
    public ApiResponse<Void> changePassword(User user, String oldPassword, String newPassword) {
        if (!passwordEncoder.matches(oldPassword, user.getPasswordHash())) {
            return ApiResponse.error("原密码错误");
        }
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
        return ApiResponse.success("密码修改成功");
    }
}
