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
import java.util.LinkedHashMap;
import java.util.List;

@RestController
@RequestMapping("/api/shop")
public class ShopController {

    private final ShopService shopService;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;

    public ShopController(ShopService shopService, ProductRepository productRepository, OrderRepository orderRepository, UserRepository userRepository) {
        this.shopService = shopService;
        this.productRepository = productRepository;
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
    }

    private User getUser(Authentication auth) {
        if (auth == null) return null;
        return userRepository.findById((Long) auth.getPrincipal()).orElse(null);
    }

    private ResponseEntity<?> mutation(ApiResponse<?> result) {
        return result.getError() == null ? ResponseEntity.ok(result) : ResponseEntity.badRequest().body(result);
    }

    // ---- Products ----
    @GetMapping("/products")
    public ResponseEntity<?> apiProducts(@RequestParam(defaultValue = "1") int page,
            @RequestParam(required = false) String species, @RequestParam(required = false) String category,
            @RequestParam(required = false) String q) {
        if (page < 1) return ResponseEntity.badRequest().body(ApiResponse.error("页码必须大于零"));
        if (q != null && !q.isBlank()) {
            var result = productRepository.searchActive(q.trim(), species, category, org.springframework.data.domain.PageRequest.of(page - 1, 12));
            return ResponseEntity.ok(Map.of("items", result.getContent().stream().map(Product::toDict).toList(),
                "page", page, "pages", result.getTotalPages(), "total", result.getTotalElements()));
        }
        return ResponseEntity.ok(shopService.getProducts(page, 12, species, category));
    }

    @GetMapping("/products/{id}")
    public ResponseEntity<?> apiProductDetail(@PathVariable Long id) {
        Product p = productRepository.findById(id).orElse(null);
        if (p == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(Map.of("product", p.toDict()));
    }

    @PostMapping("/products")
    public ResponseEntity<?> apiCreateProduct(Authentication auth, @RequestBody Map<String, Object> data) {
        User user = getUser(auth);
        if (user == null || !Set.of("admin", "merchant").contains(user.getRole()))
            return ResponseEntity.status(403).body(ApiResponse.error("需要商户权限"));
        Long merchantId = "merchant".equals(user.getRole()) ? user.getId() : null;
        return mutation(shopService.createProduct(data, merchantId));
    }

    @PutMapping("/products/{id}")
    public ResponseEntity<?> apiUpdateProduct(Authentication auth, @PathVariable Long id, @RequestBody Map<String, Object> data) {
        User user = getUser(auth);
        Product p = productRepository.findById(id).orElse(null);
        if (p == null) return ResponseEntity.notFound().build();
        if (!canManageProduct(user, p))
            return ResponseEntity.status(403).body(ApiResponse.error("无权操作此商品"));
        return mutation(shopService.updateProduct(p, data));
    }

    @DeleteMapping("/products/{id}")
    public ResponseEntity<?> apiDeleteProduct(Authentication auth, @PathVariable Long id) {
        User user = getUser(auth);
        Product p = productRepository.findById(id).orElse(null);
        if (p == null) return ResponseEntity.notFound().build();
        if (!canManageProduct(user, p)) return ResponseEntity.status(403).body(ApiResponse.error("无权操作此商品"));
        return mutation(shopService.deleteProduct(p));
    }

    @PostMapping("/products/{id}/image")
    public ResponseEntity<?> uploadProductImage(Authentication auth, @PathVariable Long id, @RequestParam("image") MultipartFile file) {
        Product p = productRepository.findById(id).orElse(null);
        if (p == null) return ResponseEntity.notFound().build();
        if (!canManageProduct(getUser(auth), p)) return ResponseEntity.status(403).body(ApiResponse.error("无权操作此商品"));
        String path = FileUtils.saveUpload(file, "products");
        if (path == null) return ResponseEntity.badRequest().body(ApiResponse.error("不支持的图片格式"));
        if (p.getImage() != null) FileUtils.deleteUpload(p.getImage());
        p.setImage(path);
        productRepository.save(p);
        return ResponseEntity.ok(ApiResponse.success("上传成功", Map.of("image_url", "/" + path)));
    }

    // ---- Cart ----
    @GetMapping("/cart")
    public ResponseEntity<?> apiGetCart(Authentication auth) {
        return ResponseEntity.ok(shopService.getCart((Long) auth.getPrincipal()));
    }

    @PostMapping("/cart")
    public ResponseEntity<?> apiAddToCart(Authentication auth, @RequestBody Map<String, Object> data) {
        if (!(data.get("product_id") instanceof Number product) || !(data.getOrDefault("quantity", 1) instanceof Number qtyValue))
            return ResponseEntity.badRequest().body(ApiResponse.error("商品与数量格式无效"));
        Long productId = product.longValue();
        int qty = qtyValue.intValue();
        return mutation(shopService.addToCart((Long) auth.getPrincipal(), productId, qty));
    }

    @DeleteMapping("/cart/{itemId}")
    public ResponseEntity<?> apiDeleteCartItem(Authentication auth, @PathVariable Long itemId) {
        return mutation(shopService.removeCartItem(itemId, (Long) auth.getPrincipal()));
    }

    @PostMapping("/cart/clear")
    public ResponseEntity<?> apiClearCart(Authentication auth) {
        return mutation(shopService.clearCart((Long) auth.getPrincipal()));
    }

    // ---- Orders ----
    @GetMapping("/orders")
    public ResponseEntity<?> apiGetOrders(Authentication auth, @RequestParam(defaultValue = "1") int page) {
        return ResponseEntity.ok(shopService.getUserOrders((Long) auth.getPrincipal(), page));
    }

    @PostMapping("/orders")
    public ResponseEntity<?> apiCheckout(Authentication auth, @RequestBody Map<String, Object> data) {
        return mutation(shopService.checkout((Long) auth.getPrincipal(), data));
    }

    @GetMapping("/orders/{id}")
    public ResponseEntity<?> apiGetOrder(Authentication auth, @PathVariable Long id) {
        Order order = shopService.getOrder(id, (Long) auth.getPrincipal());
        if (order == null) return ResponseEntity.status(404).body(ApiResponse.error("订单不存在"));
        return ResponseEntity.ok(Map.of("order", order.toDict(true)));
    }

    @PutMapping("/orders/{id}/status")
    public ResponseEntity<?> apiUpdateOrderStatus(Authentication auth, @PathVariable Long id, @RequestBody Map<String, String> data) {
        User user = getUser(auth);
        if (user == null) return ResponseEntity.status(401).body(ApiResponse.error("请先登录"));
        Order order = shopService.getOrder(id, null);
        if (order == null) return ResponseEntity.notFound().build();
        String status = data.get("status");
        if (status == null || status.isBlank()) return ResponseEntity.badRequest().body(ApiResponse.error("请指定订单状态"));
        boolean buyerCancel = user.getId().equals(order.getUser().getId()) && "cancelled".equals(status) && "pending".equals(order.getStatus());
        boolean privileged = "admin".equals(user.getRole());
        boolean merchant = "merchant".equals(user.getRole()) && !order.getItems().isEmpty() && order.getItems().stream().allMatch(i -> i.getProduct() != null && i.getProduct().getMerchant() != null && user.getId().equals(i.getProduct().getMerchant().getId()));
        if (!buyerCancel && !privileged && !merchant) return ResponseEntity.status(403).body(ApiResponse.error("无权修改订单"));
        return mutation(shopService.updateOrderStatus(id, data.get("status")));
    }

    @GetMapping("/merchant/products")
    public ResponseEntity<?> merchantProducts(Authentication auth, @RequestParam(defaultValue = "1") int page) {
        User user = getUser(auth);
        if (user == null || !Set.of("admin", "merchant").contains(user.getRole())) return ResponseEntity.status(403).body(ApiResponse.error("需要商户权限"));
        var result = productRepository.findByMerchantId(user.getId(), org.springframework.data.domain.PageRequest.of(Math.max(0, page - 1), 30));
        return ResponseEntity.ok(Map.of("items", result.getContent().stream().map(Product::toDict).toList(), "page", page, "pages", result.getTotalPages()));
    }

    @GetMapping("/merchant/orders")
    public ResponseEntity<?> merchantOrders(Authentication auth) {
        User user = getUser(auth);
        if (user == null || !"merchant".equals(user.getRole())) return ResponseEntity.status(403).body(ApiResponse.error("需要商户权限"));
        return ResponseEntity.ok(Map.of("items", orderRepository.findByMerchant(user.getId()).stream().map(o -> {
            List<OrderItem> ownItems = o.getItems().stream().filter(i -> i.getProduct() != null
                && i.getProduct().getMerchant() != null
                && user.getId().equals(i.getProduct().getMerchant().getId())).toList();
            Map<String, Object> dto = new LinkedHashMap<>(o.toDict(false));
            dto.put("items", ownItems.stream().map(OrderItem::toDict).toList());
            dto.put("item_count", ownItems.size());
            dto.put("total_amount", ownItems.stream().mapToDouble(OrderItem::getSubtotal).sum());
            dto.put("can_update_status", ownItems.size() == o.getItems().size());
            return dto;
        }).toList()));
    }

    private boolean canManageProduct(User user, Product product) {
        return user != null && ("admin".equals(user.getRole()) ||
            ("merchant".equals(user.getRole()) && product.getMerchant() != null && user.getId().equals(product.getMerchant().getId())));
    }
}
