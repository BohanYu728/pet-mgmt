package com.petmgmt.repository;

import com.petmgmt.model.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    Page<Product> findByIsActiveTrueAndTargetSpeciesIn(List<String> species, Pageable pageable);
    Page<Product> findByIsActiveTrue(Pageable pageable);
    Page<Product> findByIsActiveTrueAndCategory(String category, Pageable pageable);
    Page<Product> findByIsActiveTrueAndTargetSpeciesInAndCategory(List<String> species, String category, Pageable pageable);
    List<Product> findByIsActiveTrueAndTargetSpeciesIn(List<String> species);
    Page<Product> findByMerchantId(Long merchantId, Pageable pageable);
    long countByIsActiveTrue();
    long countByMerchantId(Long merchantId);
}
