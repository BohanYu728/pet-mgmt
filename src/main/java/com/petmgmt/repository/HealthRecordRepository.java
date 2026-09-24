package com.petmgmt.repository;

import com.petmgmt.model.HealthRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface HealthRecordRepository extends JpaRepository<HealthRecord, Long> {
    Page<HealthRecord> findByPetIdOrderByRecordDateDesc(Long petId, Pageable pageable);
    List<HealthRecord> findByPetIdOrderByRecordDateDesc(Long petId);
    List<HealthRecord> findByCreatorId(Long userId);
    long countByCreatorId(Long userId);
    long countByRecordType(String recordType);
}
