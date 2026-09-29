package com.petmgmt.controller;

import com.petmgmt.dto.AdminUserRequest;
import com.petmgmt.dto.ApiResponse;
import com.petmgmt.model.User;
import com.petmgmt.service.AdminUserService;
import jakarta.validation.Valid;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/users")
@PreAuthorize("hasAuthority('admin:manage')")
public class AdminUserController {
    private final AdminUserService service;
    public AdminUserController(AdminUserService service) { this.service=service; }

    @GetMapping public Map<String,Object> all() {
        return Map.of("users",service.all().stream().map(User::toDict).toList());
    }
    @GetMapping("/{id}") public ResponseEntity<?> one(@PathVariable Long id) {
        User user=service.find(id);
        return user==null ? ResponseEntity.notFound().build() : ResponseEntity.ok(Map.of("user",user.toDict()));
    }
    @PostMapping public ResponseEntity<?> create(@Valid @RequestBody AdminUserRequest request) {
        try { return ResponseEntity.status(201).body(Map.of("user",service.create(request).toDict())); }
        catch (IllegalArgumentException ex) { return ResponseEntity.badRequest().body(ApiResponse.error(ex.getMessage())); }
        catch (DataIntegrityViolationException ex) { return ResponseEntity.status(409).body(ApiResponse.error("用户名或邮箱已存在")); }
    }
    @PutMapping("/{id}") public ResponseEntity<?> update(@PathVariable Long id,@Valid @RequestBody AdminUserRequest request) {
        try {
            User user=service.update(id,request);
            return user==null ? ResponseEntity.notFound().build() : ResponseEntity.ok(Map.of("user",user.toDict()));
        } catch (DataIntegrityViolationException ex) {
            return ResponseEntity.status(409).body(ApiResponse.error("用户名或邮箱已存在"));
        }
    }
    @DeleteMapping("/{id}") public ResponseEntity<?> delete(@PathVariable Long id,Authentication auth) {
        try {
            return service.delete(id,(Long) auth.getPrincipal()) ? ResponseEntity.ok(ApiResponse.success("删除成功")) : ResponseEntity.notFound().build();
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ApiResponse.error(ex.getMessage()));
        } catch (IllegalStateException ex) {
            return ResponseEntity.status(409).body(ApiResponse.error(ex.getMessage()));
        }
    }
}
