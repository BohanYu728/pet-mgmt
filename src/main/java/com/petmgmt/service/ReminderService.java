package com.petmgmt.service;

import com.petmgmt.dto.ApiResponse;
import com.petmgmt.model.Reminder;
import com.petmgmt.repository.ReminderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ReminderService {

    private final ReminderRepository reminderRepository;

    public ReminderService(ReminderRepository reminderRepository) {
        this.reminderRepository = reminderRepository;
    }

    public List<Map<String, Object>> getUpcomingReminders(Long userId, int days) {
        LocalDate today = LocalDate.now();
        return reminderRepository.findByUserIdAndReminderDateBetweenAndIsDismissedFalse(
            userId, today, today.plusDays(days))
            .stream().map(Reminder::toDict).collect(Collectors.toList());
    }

    public List<Map<String, Object>> getUnreadReminders(Long userId) {
        return reminderRepository.findByUserIdAndIsReadFalseAndIsDismissedFalseOrderByReminderDateAsc(userId)
            .stream().map(Reminder::toDict).collect(Collectors.toList());
    }

    @Transactional
    public ApiResponse<Void> markRead(Long reminderId) {
        reminderRepository.findById(reminderId).ifPresent(r -> {
            r.setIsRead(true);
            reminderRepository.save(r);
        });
        return ApiResponse.success("已标记为已读");
    }

    @Transactional
    public ApiResponse<Void> dismiss(Long reminderId) {
        reminderRepository.findById(reminderId).ifPresent(r -> {
            r.setIsDismissed(true);
            reminderRepository.save(r);
        });
        return ApiResponse.success("已忽略");
    }
}
