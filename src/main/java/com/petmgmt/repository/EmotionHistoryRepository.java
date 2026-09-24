package com.petmgmt.repository;

import com.petmgmt.model.EmotionHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface EmotionHistoryRepository extends JpaRepository<EmotionHistory, Long> {
    List<EmotionHistory> findTop10ByPetIdOrderByCreatedAtDesc(Long petId);
    Page<EmotionHistory> findByPetIdOrderByCreatedAtDesc(Long petId, Pageable pageable);
    long countByUserId(Long userId);
}
