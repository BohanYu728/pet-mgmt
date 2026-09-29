package com.petmgmt.repository;

import com.petmgmt.model.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    @Modifying
    @Query("update Order o set o.status = :next where o.id = :id and o.status = :expected")
    int transitionStatusIfCurrent(@Param("id") Long id, @Param("expected") String expected, @Param("next") String next);
    Page<Order> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
    long countByUserId(Long userId);
    long countByCreatedAtBetween(java.time.LocalDateTime start, java.time.LocalDateTime end);
    @Query("select distinct o from Order o join o.items i join i.product p where p.merchant.id = :merchantId order by o.createdAt desc")
    java.util.List<Order> findByMerchant(@Param("merchantId") Long merchantId);
}
