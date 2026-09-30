package com.petmgmt.controller;

import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/demo")
public class DemoController {
    private final Environment environment;
    public DemoController(Environment environment) { this.environment = environment; }
    @GetMapping("/status")
    public Map<String, Object> status() {
        if (!environment.acceptsProfiles(Profiles.of("demo"))) return Map.of("enabled", false);
        // Public, fictitious credentials only in the isolated demo profile. Login still uses /auth/login.
        return Map.of("enabled", true, "accounts", List.of(
            Map.of("role", "user", "label", "宠物主人", "username", "demo", "password", "demo123"),
            Map.of("role", "merchant", "label", "商户", "username", "demo_shop", "password", "demoShop123"),
            Map.of("role", "admin", "label", "管理员", "username", "admin", "password", "admin123")));
    }
}
