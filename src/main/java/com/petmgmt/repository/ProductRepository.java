package com.petmgmt.repository;

import com.petmgmt.model.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    @Modifying
    @Query("update Product p set p.stock = p.stock - :quantity where p.id = :id and p.isActive = true and p.stock >= :quantity")
    int decrementStockIfAvailable(@Param("id") Long id, @Param("quantity") int quantity);

    @Modifying
    @Query("update Product p set p.stock = p.stock + :quantity where p.id = :id")
    int incrementStock(@Param("id") Long id, @Param("quantity") int quantity);
    Page<Product> findByIsActiveTrueAndTargetSpeciesIn(List<String> species, Pageable pageable);
    Page<Product> findByIsActiveTrue(Pageable pageable);
    Page<Product> findByIsActiveTrueAndCategory(String category, Pageable pageable);
    Page<Product> findByIsActiveTrueAndTargetSpeciesInAndCategory(List<String> species, String category, Pageable pageable);
    List<Product> findByIsActiveTrueAndTargetSpeciesIn(List<String> species);
    Page<Product> findByMerchantId(Long merchantId, Pageable pageable);
    long countByIsActiveTrue();
    long countByMerchantId(Long merchantId);
    @Query("select p from Product p where p.isActive = true and lower(p.name) like lower(concat('%', :q, '%')) and (:species is null or p.targetSpecies = :species or p.targetSpecies = 'both') and (:category is null or p.category = :category)")
    Page<Product> searchActive(@Param("q") String q, @Param("species") String species,
                               @Param("category") String category, Pageable pageable);
}
