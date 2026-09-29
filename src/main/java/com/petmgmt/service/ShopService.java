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
        List<String> speciesList = species != null ? ("both".equals(species) ? List.of("both") : List.of(species, "both")) : List.of("dog", "cat", "both");
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
        String invalid = validateProduct(data, true);
        if (invalid != null) return ApiResponse.error(invalid);
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
        String invalid = validateProduct(data, false);
        if (invalid != null) return ApiResponse.error(invalid);
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
        if (quantity <= 0) return ApiResponse.error("购买数量必须为正整数");
        if (userRepository.lockById(userId).isEmpty()) return ApiResponse.error("用户不存在");
        Product p = productRepository.findById(productId).orElse(null);
        if (p == null || !Boolean.TRUE.equals(p.getIsActive())) return ApiResponse.error("商品不存在或已下架");
        if (p.getStock() < quantity) return ApiResponse.error("库存不足");

        Optional<CartItem> existing = cartItemRepository.findByUserIdAndProductId(userId, productId);
        CartItem item;
        if (existing.isPresent()) {
            item = existing.get();
            if ((long) item.getQuantity() + quantity > p.getStock()) return ApiResponse.error("累计数量超过库存");
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
        if (userRepository.lockById(userId).isEmpty()) return ApiResponse.error("用户不存在");
        CartItem item = cartItemRepository.findById(itemId).orElse(null);
        if (item == null || !item.getUserId().equals(userId)) return ApiResponse.error("购物车项不存在");
        cartItemRepository.delete(item);
        return ApiResponse.success("已移除");
    }

    @Transactional
    public ApiResponse<Void> clearCart(Long userId) {
        if (userRepository.lockById(userId).isEmpty()) return ApiResponse.error("用户不存在");
        cartItemRepository.deleteByUserId(userId);
        return ApiResponse.success("购物车已清空");
    }

    // ---- Orders ----
    @Transactional
    public ApiResponse<Map<String, Object>> checkout(Long userId, Map<String, Object> data) {
        User user = userRepository.lockById(userId).orElse(null);
        if (user == null) return ApiResponse.error("用户不存在");
        List<CartItem> cartItems = cartItemRepository.findByUserId(userId);
        if (cartItems.isEmpty()) return ApiResponse.error("购物车为空");

        // Duplicate legacy rows must count together before any stock is changed.
        Map<Long, BasketLine> basket = new LinkedHashMap<>();
        for (CartItem ci : cartItems) {
            Product p = ci.getProduct();
            if (p == null || p.getId() == null || !Boolean.TRUE.equals(p.getIsActive())) return ApiResponse.error("商品已下架");
            if (ci.getQuantity() == null || ci.getQuantity() <= 0) return ApiResponse.error(p.getName() + " 数量无效");
            BasketLine previous = basket.get(p.getId());
            long totalQuantity = (long) ci.getQuantity() + (previous == null ? 0 : previous.quantity());
            if (totalQuantity > Integer.MAX_VALUE) return ApiResponse.error(p.getName() + " 数量过大");
            basket.put(p.getId(), new BasketLine(p, (int) totalQuantity));
        }
        for (BasketLine line : basket.values()) {
            if (line.product().getStock() == null || line.product().getStock() < line.quantity())
                return ApiResponse.error(line.product().getName() + " 库存不足");
        }

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
        for (BasketLine line : basket.values()) {
            Product p = line.product();
            if (productRepository.decrementStockIfAvailable(p.getId(), line.quantity()) != 1)
                throw new ShopConflictException(p.getName() + " 库存已变化，请重试下单");
            OrderItem oi = new OrderItem();
            oi.setOrder(order);
            oi.setProduct(p);
            oi.setProductName(p.getName());
            oi.setProductPrice(p.getPrice());
            oi.setQuantity(line.quantity());
            oi.setSubtotal(p.getPrice() * line.quantity());
            orderItems.add(oi);
            total += oi.getSubtotal();
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
        if (status == null || !Set.of("pending", "paid", "shipped", "delivered", "cancelled").contains(status)) {
            return ApiResponse.error("无效的状态");
        }
        if (status.equals(order.getStatus())) return ApiResponse.success("状态未改变", Map.of("order", order.toDict(true)));
        Map<String, Set<String>> transitions = Map.of(
            "pending", Set.of("paid", "cancelled"),
            "paid", Set.of("shipped", "cancelled"),
            "shipped", Set.of("delivered"),
            "delivered", Set.of(), "cancelled", Set.of());
        if (!transitions.getOrDefault(order.getStatus(), Set.of()).contains(status)) return ApiResponse.error("不允许的订单状态变更");
        if (orderRepository.transitionStatusIfCurrent(orderId, order.getStatus(), status) != 1)
            return ApiResponse.error("订单状态已变化，请刷新后重试");
        if ("cancelled".equals(status)) {
            for (OrderItem oi : order.getItems()) {
                Product p = oi.getProduct();
                if (p != null && productRepository.incrementStock(p.getId(), oi.getQuantity()) != 1)
                    throw new ShopConflictException("恢复库存失败，请重试");
            }
        }
        order.setStatus(status);
        return ApiResponse.success("状态已更新", Map.of("order", order.toDict(true)));
    }

    private record BasketLine(Product product, int quantity) {}

    private String validateProduct(Map<String, Object> data, boolean creating) {
        if (creating || data.containsKey("name")) {
            if (!(data.get("name") instanceof String name) || name.isBlank() || name.length() > 128) return "商品名称需为1至128个字符";
        }
        if (creating || data.containsKey("price")) {
            if (!(data.get("price") instanceof Number price) || !Double.isFinite(price.doubleValue()) || price.doubleValue() < 0) return "价格必须为非负数";
        }
        if (data.containsKey("stock")) {
            if (!(data.get("stock") instanceof Number stock) || !Double.isFinite(stock.doubleValue()) || stock.doubleValue() < 0 || stock.doubleValue() > Integer.MAX_VALUE || stock.doubleValue() != stock.intValue()) return "库存必须为非负整数";
        }
        if (data.containsKey("category") && (!(data.get("category") instanceof String category) || !Set.of("food", "toy", "accessory", "medicine", "other").contains(category))) return "无效的商品分类";
        if (data.containsKey("target_species") && (!(data.get("target_species") instanceof String species) || !Set.of("dog", "cat", "both").contains(species))) return "无效的适用物种";
        if (data.containsKey("is_active") && !(data.get("is_active") instanceof Boolean)) return "上架状态必须为布尔值";
        if (data.containsKey("description") && !(data.get("description") instanceof String)) return "商品描述必须为文本";
        return null;
    }
}
