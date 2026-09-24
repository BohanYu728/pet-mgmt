package com.petmgmt.repository;

import com.petmgmt.model.Reminder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface ReminderRepository extends JpaRepository<Reminder, Long> {
    List<Reminder> findByUserIdAndIsDismissedFalseOrderByReminderDateAsc(Long userId);
    List<Reminder> findByUserIdAndReminderDateBetweenAndIsDismissedFalse(Long userId, LocalDate start, LocalDate end);
    List<Reminder> findByUserIdAndIsReadFalseAndIsDismissedFalseOrderByReminderDateAsc(Long userId);
    long countByUserIdAndIsReadFalseAndIsDismissedFalse(Long userId);
    List<Reminder> findByPetId(Long petId);
}
