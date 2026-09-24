package com.petmgmt.repository;

import com.petmgmt.model.WeightRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface WeightRecordRepository extends JpaRepository<WeightRecord, Long> {
    List<WeightRecord> findByPetIdOrderByRecordDateDesc(Long petId);
    List<WeightRecord> findTop10ByPetIdOrderByRecordDateDesc(Long petId);
}
