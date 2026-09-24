package com.petmgmt.repository;

import com.petmgmt.model.RentalRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface RentalRecordRepository extends JpaRepository<RentalRecord, Long> {
    List<RentalRecord> findByMerchantIdOrderByCreatedAtDesc(Long merchantId);
}
