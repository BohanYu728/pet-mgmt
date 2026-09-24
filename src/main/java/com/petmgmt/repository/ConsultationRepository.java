package com.petmgmt.repository;

import com.petmgmt.model.Consultation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ConsultationRepository extends JpaRepository<Consultation, Long> {
    Page<Consultation> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
}
