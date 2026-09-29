package com.petmgmt.service;

import com.petmgmt.model.*;
import com.petmgmt.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ShopServiceTest {
    ProductRepository products = mock(ProductRepository.class);
    CartItemRepository carts = mock(CartItemRepository.class);
    OrderRepository orders = mock(OrderRepository.class);
    UserRepository users = mock(UserRepository.class);
    ShopService service = new ShopService(products, carts, orders, users);
    Product product;

    @BeforeEach void setUp() {
        product = Product.builder().id(1L).name("猫粮").stock(5).price(10.0).isActive(true).build();
        when(products.findById(1L)).thenReturn(Optional.of(product));
        when(users.lockById(1L)).thenReturn(Optional.of(User.builder().id(1L).build()));
    }

    @Test void refusesNegativeCartQuantity() {
        assertNotNull(service.addToCart(1L, 1L, -1).getError());
        verify(carts, never()).save(any());
    }

    @Test void cumulativeCartQuantityCannotExceedStock() {
        CartItem existing = new CartItem(); existing.setQuantity(4); existing.setProduct(product);
        when(carts.findByUserIdAndProductId(1L, 1L)).thenReturn(Optional.of(existing));
        assertNotNull(service.addToCart(1L, 1L, 2).getError());
        assertEquals(4, existing.getQuantity());
    }

    @Test void failedCheckoutDoesNotConsumeEarlierProducts() {
        Product unavailable = Product.builder().id(2L).name("玩具").stock(0).price(5.0).isActive(true).build();
        CartItem first = new CartItem(); first.setProduct(product); first.setQuantity(1);
        CartItem second = new CartItem(); second.setProduct(unavailable); second.setQuantity(1);
        when(carts.findByUserId(1L)).thenReturn(List.of(first, second));
        when(users.findById(1L)).thenReturn(Optional.of(User.builder().id(1L).build()));
        assertNotNull(service.checkout(1L, Map.of()).getError());
        assertEquals(5, product.getStock());
        verify(products, never()).save(any());
    }

    @Test void duplicateCartRowsCannotConsumeStockWhenCombinedQuantityExceedsStock() {
        CartItem first = new CartItem(); first.setProduct(product); first.setQuantity(3);
        CartItem second = new CartItem(); second.setProduct(product); second.setQuantity(3);
        when(carts.findByUserId(1L)).thenReturn(List.of(first, second));
        when(users.findById(1L)).thenReturn(Optional.of(User.builder().id(1L).build()));
        assertNotNull(service.checkout(1L, Map.of()).getError());
        assertEquals(5, product.getStock());
        verify(products, never()).save(any());
        verify(orders, never()).save(any());
    }

    @Test void cancellingTwiceRestocksOnlyOnce() {
        Order order = Order.builder().id(1L).status("pending").build();
        OrderItem item = new OrderItem(); item.setProduct(product); item.setQuantity(2);
        order.setItems(List.of(item));
        when(orders.findById(1L)).thenReturn(Optional.of(order));
        when(orders.transitionStatusIfCurrent(1L, "pending", "cancelled")).thenReturn(1);
        when(products.incrementStock(1L, 2)).thenReturn(1);
        service.updateOrderStatus(1L, "cancelled");
        service.updateOrderStatus(1L, "cancelled");
        verify(products, times(1)).incrementStock(1L, 2);
    }

    @Test void cancelledOrderCannotReturnToPaid() {
        when(orders.findById(1L)).thenReturn(Optional.of(Order.builder().id(1L).status("cancelled").build()));
        assertNotNull(service.updateOrderStatus(1L, "paid").getError());
    }

    @Test void missingStatusReturnsValidationError() {
        when(orders.findById(1L)).thenReturn(Optional.of(Order.builder().id(1L).status("pending").build()));
        assertNotNull(service.updateOrderStatus(1L, null).getError());
    }

    @Test void negativeProductPriceIsRejected() {
        assertNotNull(service.createProduct(Map.of("name", "猫粮", "price", -1, "stock", 1), 1L).getError());
        verify(products, never()).save(any());
    }

    @Test void invalidUpdateDoesNotMutateProduct() {
        assertNotNull(service.updateProduct(product, Map.of("name", "新名字", "stock", -1)).getError());
        assertEquals("猫粮", product.getName());
        assertEquals(5, product.getStock());
    }

    @Test void speciesFilterIncludesProductsForBothSpecies() {
        when(products.findByIsActiveTrueAndTargetSpeciesIn(anyList(), any(org.springframework.data.domain.Pageable.class)))
            .thenReturn(org.springframework.data.domain.Page.empty());
        service.getProducts(1, 12, "dog", null);
        verify(products).findByIsActiveTrueAndTargetSpeciesIn(eq(List.of("dog", "both")), any(org.springframework.data.domain.Pageable.class));
    }
}
