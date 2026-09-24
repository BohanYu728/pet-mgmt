package com.petmgmt.repository;

import com.petmgmt.model.BreedHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BreedHistoryRepository extends JpaRepository<BreedHistory, Long> {
    Page<BreedHistory> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
    long countByUserId(Long userId);
}
