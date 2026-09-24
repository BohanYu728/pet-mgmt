package com.petmgmt.service;

import com.petmgmt.dto.ApiResponse;
import com.petmgmt.model.*;
import com.petmgmt.repository.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class ShopService {

    private final ProductRepository productRepository;
    private final CartItemRepository cartItemRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;

    public ShopService(ProductRepository productRepository, CartItemRepository cartItemRepository,
                       OrderRepository orderRepository, UserRepository userRepository) {
        this.productRepository = productRepository;
        this.cartItemRepository = cartItemRepository;
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
    }

    // ---- Products ----
    public Map<String, Object> getProducts(int page, int perPage, String species, String category) {
        PageRequest pr = PageRequest.of(page - 1, perPage);
        Page<Product> result;
        List<String> speciesList = species != null ? List.of(species) : List.of("dog", "cat", "both");
        if (category != null && species != null) {
            result = productRepository.findByIsActiveTrueAndTargetSpeciesInAndCategory(speciesList, category, pr);
        } else if (category != null) {
            result = productRepository.findByIsActiveTrueAndCategory(category, pr);
        } else if (species != null) {
            result = productRepository.findByIsActiveTrueAndTargetSpeciesIn(speciesList, pr);
        } else {
            result = productRepository.findByIsActiveTrue(pr);
        }
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("items", result.getContent().stream().map(Product::toDict).toList());
        map.put("page", page); map.put("pages", result.getTotalPages());
        map.put("total", result.getTotalElements());
        map.put("has_next", result.hasNext()); map.put("has_prev", result.hasPrevious());
        return map;
    }

    @Transactional
    public ApiResponse<Map<String, Object>> createProduct(Map<String, Object> data, Long merchantId) {
        Product p = new Product();
        p.setName((String) data.get("name"));
        p.setCategory((String) data.getOrDefault("category", "food"));
        p.setTargetSpecies((String) data.getOrDefault("target_species", "both"));
        p.setDescription((String) data.getOrDefault("description", ""));
        p.setPrice(((Number) data.getOrDefault("price", 0)).doubleValue());
        p.setStock(((Number) data.getOrDefault("stock", 0)).intValue());
        p.setIsActive((Boolean) data.getOrDefault("is_active", true));
        if (merchantId != null) p.setMerchant(userRepository.findById(merchantId).orElse(null));
        productRepository.save(p);
        return ApiResponse.success("添加成功", Map.of("product", p.toDict()));
    }

    @Transactional
    public ApiResponse<Map<String, Object>> updateProduct(Product p, Map<String, Object> data) {
        if (data.containsKey("name")) p.setName((String) data.get("name"));
        if (data.containsKey("category")) p.setCategory((String) data.get("category"));
        if (data.containsKey("target_species")) p.setTargetSpecies((String) data.get("target_species"));
        if (data.containsKey("description")) p.setDescription((String) data.get("description"));
        if (data.containsKey("price")) p.setPrice(((Number) data.get("price")).doubleValue());
        if (data.containsKey("stock")) p.setStock(((Number) data.get("stock")).intValue());
        if (data.containsKey("is_active")) p.setIsActive((Boolean) data.get("is_active"));
        productRepository.save(p);
        return ApiResponse.success("更新成功", Map.of("product", p.toDict()));
    }

    @Transactional
    public ApiResponse<Void> deleteProduct(Product p) {
        if (p.getImage() != null) com.petmgmt.util.FileUtils.deleteUpload(p.getImage());
        productRepository.delete(p);
        return ApiResponse.success("删除成功");
    }

    // ---- Cart ----
    public Map<String, Object> getCart(Long userId) {
        List<CartItem> items = cartItemRepository.findByUserId(userId);
        double total = items.stream().mapToDouble(i -> i.getProduct().getPrice() * i.getQuantity()).sum();
        return Map.of("items", items.stream().map(CartItem::toDict).toList(),
            "total_amount", total, "item_count", items.size());
    }

    @Transactional
    public ApiResponse<Map<String, Object>> addToCart(Long userId, Long productId, int quantity) {
        Product p = productRepository.findById(productId).orElse(null);
        if (p == null || !Boolean.TRUE.equals(p.getIsActive())) return ApiResponse.error("商品不存在或已下架");
        if (p.getStock() < quantity) return ApiResponse.error("库存不足");

        Optional<CartItem> existing = cartItemRepository.findByUserIdAndProductId(userId, productId);
        CartItem item;
        if (existing.isPresent()) {
            item = existing.get();
            item.setQuantity(item.getQuantity() + quantity);
        } else {
            item = new CartItem();
            item.setUserId(userId);
            item.setProduct(p);
            item.setQuantity(quantity);
        }
        cartItemRepository.save(item);
        return ApiResponse.success("已加入购物车", Map.of("item", item.toDict()));
    }

    @Transactional
    public ApiResponse<Void> removeCartItem(Long itemId, Long userId) {
        CartItem item = cartItemRepository.findById(itemId).orElse(null);
        if (item == null || !item.getUserId().equals(userId)) return ApiResponse.error("购物车项不存在");
        cartItemRepository.delete(item);
        return ApiResponse.success("已移除");
    }

    @Transactional
    public ApiResponse<Void> clearCart(Long userId) {
        cartItemRepository.deleteByUserId(userId);
        return ApiResponse.success("购物车已清空");
    }

    // ---- Orders ----
    @Transactional
    public ApiResponse<Map<String, Object>> checkout(Long userId, Map<String, Object> data) {
        List<CartItem> cartItems = cartItemRepository.findByUserId(userId);
        if (cartItems.isEmpty()) return ApiResponse.error("购物车为空");

        User user = userRepository.findById(userId).orElse(null);

        Order order = new Order();
        order.setUser(user);
        order.setStatus("pending");
        order.setNote((String) data.getOrDefault("note", ""));
        order.setRecipientName((String) data.getOrDefault("recipient_name", ""));
        order.setRecipientPhone((String) data.getOrDefault("recipient_phone", ""));
        order.setAddressProvince((String) data.getOrDefault("address_province", ""));
        order.setAddressCity((String) data.getOrDefault("address_city", ""));
        order.setAddressDistrict((String) data.getOrDefault("address_district", ""));
        order.setAddressDetail((String) data.getOrDefault("address_detail", ""));

        double total = 0;
        List<OrderItem> orderItems = new ArrayList<>();
        for (CartItem ci : cartItems) {
            Product p = ci.getProduct();
            if (p.getStock() < ci.getQuantity()) return ApiResponse.error(p.getName() + " 库存不足");
            OrderItem oi = new OrderItem();
            oi.setOrder(order);
            oi.setProduct(p);
            oi.setProductName(p.getName());
            oi.setProductPrice(p.getPrice());
            oi.setQuantity(ci.getQuantity());
            oi.setSubtotal(p.getPrice() * ci.getQuantity());
            orderItems.add(oi);
            total += oi.getSubtotal();
            p.setStock(p.getStock() - ci.getQuantity());
            productRepository.save(p);
        }

        order.setTotalAmount(total);
        order.setItems(orderItems);
        orderRepository.save(order);
        cartItemRepository.deleteByUserId(userId);

        return ApiResponse.success("下单成功", Map.of("order", order.toDict(true)));
    }

    public Map<String, Object> getUserOrders(Long userId, int page) {
        PageRequest pr = PageRequest.of(page - 1, 10);
        Page<Order> orders = orderRepository.findByUserIdOrderByCreatedAtDesc(userId, pr);
        return Map.of("items", orders.getContent().stream().map(o -> o.toDict(false)).toList(),
            "page", page, "pages", orders.getTotalPages(), "total", orders.getTotalElements());
    }

    public Order getOrder(Long orderId, Long userId) {
        Order order = orderRepository.findById(orderId).orElse(null);
        if (order != null && userId != null && !order.getUser().getId().equals(userId)) return null;
        return order;
    }

    @Transactional
    public ApiResponse<Map<String, Object>> updateOrderStatus(Long orderId, String status) {
        Order order = orderRepository.findById(orderId).orElse(null);
        if (order == null) return ApiResponse.error("订单不存在");
        if (!Set.of("pending", "paid", "shipped", "delivered", "cancelled").contains(status)) {
            return ApiResponse.error("无效的状态");
        }
        if ("cancelled".equals(status)) {
            for (OrderItem oi : order.getItems()) {
                Product p = oi.getProduct();
                if (p != null) { p.setStock(p.getStock() + oi.getQuantity()); productRepository.save(p); }
            }
        }
        order.setStatus(status);
        orderRepository.save(order);
        return ApiResponse.success("状态已更新", Map.of("order", order.toDict(true)));
    }
}
