package com.petmgmt.controller;

import com.petmgmt.model.*;
import com.petmgmt.repository.*;
import com.petmgmt.security.JwtTokenProvider;
import jakarta.servlet.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.*;

@Controller
public class PageController {

    private final UserRepository userRepository;
    private final PetRepository petRepository;
    private final ReminderRepository reminderRepository;
    private final HospitalRepository hospitalRepository;
    private final ProductRepository productRepository;
    private final JwtTokenProvider jwtTokenProvider;

    public PageController(UserRepository ur, PetRepository pr, ReminderRepository rr,
                          HospitalRepository hr, ProductRepository prodR, JwtTokenProvider jp) {
        this.userRepository = ur; this.petRepository = pr; this.reminderRepository = rr;
        this.hospitalRepository = hr; this.productRepository = prodR; this.jwtTokenProvider = jp;
    }

    private User resolveUser(HttpServletRequest request) {
        String token = null;
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie c : cookies) {
                if ("token".equals(c.getName())) { token = c.getValue(); break; }
            }
        }
        if (token == null) {
            String authHeader = request.getHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) token = authHeader.substring(7);
        }
        if (token != null && jwtTokenProvider.validateToken(token)) {
            return userRepository.findById(jwtTokenProvider.getUserIdFromToken(token)).orElse(null);
        }
        return null;
    }

    private void addCommon(Model model, HttpServletRequest request) {
        model.addAttribute("user", resolveUser(request));
        model.addAttribute("currentYear", LocalDate.now().getYear());
    }

    @GetMapping("/")
    public String index(HttpServletRequest request, Model model) {
        User user = resolveUser(request);
        if (user != null) return "redirect:/dashboard";
        model.addAttribute("user", null);
        model.addAttribute("currentYear", LocalDate.now().getYear());
        return "index";
    }

    @GetMapping("/dashboard")
    public String dashboard(HttpServletRequest request, Model model) {
        User user = resolveUser(request);
        if (user == null) return "redirect:/auth/login";
        List<Pet> pets = petRepository.findByOwnerId(user.getId());
        model.addAttribute("user", user);
        model.addAttribute("pets", pets);
        model.addAttribute("petCount", pets.size());
        model.addAttribute("currentYear", LocalDate.now().getYear());
        return "dashboard";
    }

    @GetMapping("/about") public String about(HttpServletRequest r, Model m) { addCommon(m, r); return "about"; }

    // Auth pages
    @GetMapping("/auth/login") public String login(Model m) { m.addAttribute("user", null); m.addAttribute("currentYear", LocalDate.now().getYear()); return "auth/login"; }
    @GetMapping("/auth/register") public String reg(Model m) { m.addAttribute("user", null); m.addAttribute("currentYear", LocalDate.now().getYear()); return "auth/register"; }
    @GetMapping("/profile") public String profile(HttpServletRequest r, Model m) { addCommon(m, r); return "auth/profile"; }

    // Pets pages
    @GetMapping("/pets") public String petsPage(HttpServletRequest r, Model m) { addCommon(m, r); return "pet/list"; }
    @GetMapping("/pets/add") public String petAdd(HttpServletRequest r, Model m) { addCommon(m, r); return "pet/form"; }
    @GetMapping("/pets/{id}") public String petDetail(@PathVariable Long id, HttpServletRequest r, Model m) {
        addCommon(m, r); m.addAttribute("petId", id); return "pet/detail";
    }

    // AI pages
    @GetMapping("/ai/breed") public String aiBreed(HttpServletRequest r, Model m) { addCommon(m, r); return "ai/breed"; }
    @GetMapping("/ai/emotion") public String aiEmotion(HttpServletRequest r, Model m) { addCommon(m, r); return "ai/emotion"; }
    @GetMapping("/ai/health-report") public String aiHealth(HttpServletRequest r, Model m) { addCommon(m, r); return "ai/health_report"; }
    @GetMapping("/ai/recommend") public String aiRec(HttpServletRequest r, Model m) { addCommon(m, r); return "ai/recommend"; }

    // Hospital pages
    @GetMapping("/hospitals") public String hospitals(HttpServletRequest r, Model m) { addCommon(m, r); m.addAttribute("hospitals", hospitalRepository.findAll()); return "hospital/map"; }

    // Shop pages
    @GetMapping("/shop") public String shop(HttpServletRequest r, Model m) { addCommon(m, r); return "shop/list"; }
    @GetMapping("/shop/cart") public String cart(HttpServletRequest r, Model m) { addCommon(m, r); return "shop/cart"; }
    @GetMapping("/shop/orders") public String orders(HttpServletRequest r, Model m) { addCommon(m, r); return "shop/orders"; }

    // Consultation
    @GetMapping("/consultation") public String consultation(HttpServletRequest r, Model m) { addCommon(m, r); return "consultation/index"; }

    // Admin pages
    @GetMapping("/admin") public String admin(HttpServletRequest r, Model m) { addCommon(m, r); return "admin/dashboard"; }
    @GetMapping("/admin/users") public String adminUsers(HttpServletRequest r, Model m) { addCommon(m, r); return "admin/users"; }
    @GetMapping("/admin/pets") public String adminPets(HttpServletRequest r, Model m) { addCommon(m, r); return "admin/pets"; }
    @GetMapping("/admin/merchants") public String adminMerchants(HttpServletRequest r, Model m) { addCommon(m, r); return "admin/merchants"; }

    // Merchant pages
    @GetMapping("/merchant") public String merchant(HttpServletRequest r, Model m) { addCommon(m, r); return "merchant/dashboard"; }
    @GetMapping("/merchant/products") public String merchantProds(HttpServletRequest r, Model m) { addCommon(m, r); return "merchant/products"; }
    @GetMapping("/merchant/orders") public String merchantOrders(HttpServletRequest r, Model m) { addCommon(m, r); return "merchant/orders"; }

    // Health
    @GetMapping("/health/pet/{petId}") public String healthList(@PathVariable Long petId, HttpServletRequest r, Model m) { addCommon(m, r); m.addAttribute("petId", petId); return "health/list"; }

    // Photos / Gallery
    @GetMapping("/photos") public String photos(HttpServletRequest r, Model m) { addCommon(m, r); return "gallery"; }

    // Logs / Diary
    @GetMapping("/logs") public String logs(HttpServletRequest r, Model m) { addCommon(m, r); return "diary"; }

    // Reminders
    @GetMapping("/reminders") public String reminders(HttpServletRequest r, Model m) { addCommon(m, r); return "reminder/list"; }
}
