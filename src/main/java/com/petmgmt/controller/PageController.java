package com.petmgmt.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/** Redirects old Thymeleaf page URLs to the supported Vue client. */
@Controller
public class PageController {
    private final String frontend;

    public PageController(@Value("${app.frontend-url:http://localhost:5173}") String frontend) {
        this.frontend = frontend.replaceAll("/+$", "");
    }

    private String go(String path) { return "redirect:" + frontend + path; }

    @GetMapping({"/", "/about", "/dashboard"})
    public String dashboard() { return go("/dashboard"); }

    @GetMapping("/auth/login") public String login() { return go("/login"); }
    @GetMapping("/auth/register") public String register() { return go("/register"); }
    @GetMapping("/profile") public String profile() { return go("/profile"); }

    @GetMapping({"/pets", "/pets/add"}) public String pets() { return go("/pets"); }
    @GetMapping("/pets/{id}") public String pet(@PathVariable Long id) { return go("/pets/" + id + "/health"); }
    @GetMapping("/health/pet/{id}") public String health(@PathVariable Long id) { return go("/pets/" + id + "/health"); }
    @GetMapping("/photos") public String photos() { return go("/pets"); }
    @GetMapping("/logs") public String logs() { return go("/pets"); }
    @GetMapping("/reminders") public String reminders() { return go("/reminders"); }

    @GetMapping("/ai/breed") public String breed() { return go("/ai/breed"); }
    @GetMapping("/ai/emotion") public String emotion() { return go("/ai/emotion"); }
    @GetMapping("/ai/health-report") public String healthReport() { return go("/ai/health"); }
    @GetMapping("/ai/recommend") public String recommend() { return go("/ai/recommend"); }
    @GetMapping("/consultation") public String consultation() { return go("/ai/consultation"); }

    @GetMapping("/hospitals") public String hospitals() { return go("/community/hospitals"); }
    @GetMapping("/shop") public String shop() { return go("/shop/products"); }
    @GetMapping("/shop/cart") public String cart() { return go("/shop/cart"); }
    @GetMapping("/shop/orders") public String orders() { return go("/shop/orders"); }

    @GetMapping("/admin") public String admin() { return go("/admin/dashboard"); }
    @GetMapping("/admin/users") public String adminUsers() { return go("/admin/users"); }
    @GetMapping("/admin/pets") public String adminPets() { return go("/admin/pets"); }
    @GetMapping("/admin/merchants") public String adminMerchants() { return go("/admin/merchants"); }
    @GetMapping("/merchant") public String merchant() { return go("/merchant/dashboard"); }
    @GetMapping("/merchant/products") public String merchantProducts() { return go("/merchant/products"); }
    @GetMapping("/merchant/orders") public String merchantOrders() { return go("/merchant/orders"); }
}
