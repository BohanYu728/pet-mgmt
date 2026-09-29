package com.petmgmt.service;

import com.petmgmt.model.*;
import com.petmgmt.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest(properties = {"spring.jpa.hibernate.ddl-auto=create-drop", "spring.sql.init.mode=never",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.jpa.show-sql=false", "logging.level.org.hibernate=ERROR"})
@Import(ShopService.class)
class ShopConcurrencyIntegrationTest {
    @Autowired ShopService shop;
    @Autowired UserRepository users;
    @Autowired ProductRepository products;
    @Autowired CartItemRepository carts;
    @Autowired OrderRepository orders;

    @Test @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void concurrentCheckoutCannotSellOneUnitTwice() throws Exception {
        long ordersBefore = orders.count();
        Product product = products.save(Product.builder().name("并发库存").price(12.0).stock(1).isActive(true).build());
        User alice = user("checkout-alice");
        User bob = user("checkout-bob");
        cart(alice, product, 1);
        cart(bob, product, 1);

        var results = parallel(() -> checkoutSucceeded(alice.getId()),
                () -> checkoutSucceeded(bob.getId()));

        assertEquals(1, results.stream().filter(Boolean::booleanValue).count());
        assertEquals(ordersBefore + 1, orders.count());
        assertEquals(0, products.findById(product.getId()).orElseThrow().getStock());
    }

    @Test @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void simultaneousCancellationRestocksOnlyOnce() throws Exception {
        Product product = products.save(Product.builder().name("退货库存").price(12.0).stock(0).isActive(true).build());
        Order order = new Order();
        order.setUser(user("cancel-buyer"));
        order.setStatus("pending");
        OrderItem item = new OrderItem();
        item.setOrder(order); item.setProduct(product); item.setProductName(product.getName());
        item.setProductPrice(12.0); item.setQuantity(1); item.setSubtotal(12.0);
        order.setItems(List.of(item));
        order = orders.save(order);
        Long orderId = order.getId();

        parallel(() -> shop.updateOrderStatus(orderId, "cancelled").getError() == null,
                () -> shop.updateOrderStatus(orderId, "cancelled").getError() == null);

        assertEquals("cancelled", orders.findById(orderId).orElseThrow().getStatus());
        assertEquals(1, products.findById(product.getId()).orElseThrow().getStock());
    }

    private User user(String name) {
        return users.save(User.builder().username(name).email(name + "@example.invalid").passwordHash("test-hash").build());
    }

    private void cart(User user, Product product, int quantity) {
        CartItem item = new CartItem();
        item.setUserId(user.getId()); item.setProduct(product); item.setQuantity(quantity);
        carts.save(item);
    }

    private boolean checkoutSucceeded(Long userId) {
        try {
            return shop.checkout(userId, Map.of()).getError() == null;
        } catch (ShopConflictException conflict) {
            return false;
        }
    }

    private static <T> List<T> parallel(Callable<T> first, Callable<T> second) throws Exception {
        CountDownLatch start = new CountDownLatch(1);
        var executor = Executors.newFixedThreadPool(2);
        try {
            var a = executor.submit(() -> { start.await(); return first.call(); });
            var b = executor.submit(() -> { start.await(); return second.call(); });
            start.countDown();
            return List.of(a.get(20, TimeUnit.SECONDS), b.get(20, TimeUnit.SECONDS));
        } finally {
            executor.shutdownNow();
        }
    }
}
