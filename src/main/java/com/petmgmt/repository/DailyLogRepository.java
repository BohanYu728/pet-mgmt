package com.petmgmt.repository;

import com.petmgmt.model.DailyLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface DailyLogRepository extends JpaRepository<DailyLog, Long> {
    Page<DailyLog> findByPetIdOrderByLogDateDesc(Long petId, Pageable pageable);
    List<DailyLog> findByPetIdAndLogDateBetween(Long petId, LocalDate start, LocalDate end);
}
