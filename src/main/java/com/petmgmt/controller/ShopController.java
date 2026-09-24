package com.petmgmt.controller;

import com.petmgmt.dto.ApiResponse;
import com.petmgmt.model.*;
import com.petmgmt.repository.*;
import com.petmgmt.service.ShopService;
import com.petmgmt.util.FileUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/shop")
public class ShopController {

    private final ShopService shopService;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    public ShopController(ShopService shopService, ProductRepository productRepository, UserRepository userRepository) {
        this.shopService = shopService;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
    }

    private User getUser(Authentication auth) {
        if (auth == null) return null;
        return userRepository.findById((Long) auth.getPrincipal()).orElse(null);
    }

    // ---- Products ----
    @GetMapping("/api/products")
    public ResponseEntity<?> apiProducts(@RequestParam(defaultValue = "1") int page,
            @RequestParam(required = false) String species, @RequestParam(required = false) String category) {
        return ResponseEntity.ok(shopService.getProducts(page, 12, species, category));
    }

    @GetMapping("/api/products/{id}")
    public ResponseEntity<?> apiProductDetail(@PathVariable Long id) {
        Product p = productRepository.findById(id).orElse(null);
        if (p == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(Map.of("product", p.toDict()));
    }

    @PostMapping("/api/products")
    public ResponseEntity<?> apiCreateProduct(Authentication auth, @RequestBody Map<String, Object> data) {
        User user = getUser(auth);
        if (user == null || !Set.of("admin", "merchant").contains(user.getRole()))
            return ResponseEntity.status(403).body(ApiResponse.error("需要商户权限"));
        Long merchantId = "merchant".equals(user.getRole()) ? user.getId() : null;
        return ResponseEntity.ok(shopService.createProduct(data, merchantId));
    }

    @PutMapping("/api/products/{id}")
    public ResponseEntity<?> apiUpdateProduct(Authentication auth, @PathVariable Long id, @RequestBody Map<String, Object> data) {
        User user = getUser(auth);
        Product p = productRepository.findById(id).orElse(null);
        if (p == null) return ResponseEntity.notFound().build();
        if ("merchant".equals(user.getRole()) && !p.getMerchant().getId().equals(user.getId()))
            return ResponseEntity.status(403).body(ApiResponse.error("无权操作此商品"));
        return ResponseEntity.ok(shopService.updateProduct(p, data));
    }

    @DeleteMapping("/api/products/{id}")
    public ResponseEntity<?> apiDeleteProduct(Authentication auth, @PathVariable Long id) {
        User user = getUser(auth);
        Product p = productRepository.findById(id).orElse(null);
        if (p == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(shopService.deleteProduct(p));
    }

    @PostMapping("/api/products/{id}/image")
    public ResponseEntity<?> uploadProductImage(Authentication auth, @PathVariable Long id, @RequestParam("image") MultipartFile file) {
        Product p = productRepository.findById(id).orElse(null);
        if (p == null) return ResponseEntity.notFound().build();
        String path = FileUtils.saveUpload(file, "products");
        if (path == null) return ResponseEntity.badRequest().body(ApiResponse.error("不支持的图片格式"));
        if (p.getImage() != null) FileUtils.deleteUpload(p.getImage());
        p.setImage(path);
        productRepository.save(p);
        return ResponseEntity.ok(ApiResponse.success("上传成功", Map.of("image_url", "/" + path)));
    }

    // ---- Cart ----
    @GetMapping("/api/cart")
    public ResponseEntity<?> apiGetCart(Authentication auth) {
        return ResponseEntity.ok(shopService.getCart((Long) auth.getPrincipal()));
    }

    @PostMapping("/api/cart")
    public ResponseEntity<?> apiAddToCart(Authentication auth, @RequestBody Map<String, Object> data) {
        Long productId = Long.valueOf(((Number) data.get("product_id")).longValue());
        int qty = ((Number) data.getOrDefault("quantity", 1)).intValue();
        return ResponseEntity.ok(shopService.addToCart((Long) auth.getPrincipal(), productId, qty));
    }

    @DeleteMapping("/api/cart/{itemId}")
    public ResponseEntity<?> apiDeleteCartItem(Authentication auth, @PathVariable Long itemId) {
        return ResponseEntity.ok(shopService.removeCartItem(itemId, (Long) auth.getPrincipal()));
    }

    @PostMapping("/api/cart/clear")
    public ResponseEntity<?> apiClearCart(Authentication auth) {
        return ResponseEntity.ok(shopService.clearCart((Long) auth.getPrincipal()));
    }

    // ---- Orders ----
    @GetMapping("/api/orders")
    public ResponseEntity<?> apiGetOrders(Authentication auth, @RequestParam(defaultValue = "1") int page) {
        return ResponseEntity.ok(shopService.getUserOrders((Long) auth.getPrincipal(), page));
    }

    @PostMapping("/api/orders")
    public ResponseEntity<?> apiCheckout(Authentication auth, @RequestBody Map<String, Object> data) {
        return ResponseEntity.ok(shopService.checkout((Long) auth.getPrincipal(), data));
    }

    @GetMapping("/api/orders/{id}")
    public ResponseEntity<?> apiGetOrder(Authentication auth, @PathVariable Long id) {
        Order order = shopService.getOrder(id, (Long) auth.getPrincipal());
        if (order == null) return ResponseEntity.status(404).body(ApiResponse.error("订单不存在"));
        return ResponseEntity.ok(Map.of("order", order.toDict(true)));
    }

    @PutMapping("/api/orders/{id}/status")
    public ResponseEntity<?> apiUpdateOrderStatus(@PathVariable Long id, @RequestBody Map<String, String> data) {
        return ResponseEntity.ok(shopService.updateOrderStatus(id, data.get("status")));
    }
}
