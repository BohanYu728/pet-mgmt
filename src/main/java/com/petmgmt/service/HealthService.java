package com.petmgmt.service;

import com.petmgmt.dto.ApiResponse;
import com.petmgmt.model.*;
import com.petmgmt.repository.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@Service
public class HealthService {

    private final HealthRecordRepository healthRecordRepository;
    private final ReminderRepository reminderRepository;

    public HealthService(HealthRecordRepository healthRecordRepository, ReminderRepository reminderRepository) {
        this.healthRecordRepository = healthRecordRepository;
        this.reminderRepository = reminderRepository;
    }

    @Transactional
    public ApiResponse<Map<String, Object>> createHealthRecord(User creator, Pet pet, Map<String, Object> data) {
        HealthRecord hr = new HealthRecord();
        hr.setPet(pet);
        hr.setCreator(creator);
        hr.setRecordType((String) data.get("record_type"));
        hr.setTitle((String) data.get("title"));
        hr.setDescription((String) data.getOrDefault("description", ""));
        hr.setVeterinarian((String) data.getOrDefault("veterinarian", ""));
        hr.setNotes((String) data.getOrDefault("notes", ""));
        hr.setAttachmentPath((String) data.getOrDefault("attachment_path", ""));

        String recordDate = (String) data.get("record_date");
        if (recordDate != null && !recordDate.isEmpty()) {
            hr.setRecordDate(LocalDate.parse(recordDate));
        } else {
            hr.setRecordDate(LocalDate.now());
        }

        String nextDueDate = (String) data.get("next_due_date");
        if (nextDueDate != null && !nextDueDate.isEmpty()) {
            hr.setNextDueDate(LocalDate.parse(nextDueDate));
        }

        healthRecordRepository.save(hr);

        // Auto-create reminder
        if (hr.getNextDueDate() != null) {
            Reminder r = new Reminder();
            r.setUser(creator);
            r.setPet(pet);
            r.setHealthRecord(hr);
            r.setTitle(hr.getTitle() + " - 到期提醒");
            r.setReminderType(hr.getRecordType());
            r.setReminderDate(hr.getNextDueDate());
            reminderRepository.save(r);
        }

        return ApiResponse.success("添加成功", Map.of("record", hr.toDict()));
    }

    @Transactional
    public ApiResponse<Map<String, Object>> updateHealthRecord(HealthRecord hr, Map<String, Object> data) {
        if (data.containsKey("record_type")) hr.setRecordType((String) data.get("record_type"));
        if (data.containsKey("title")) hr.setTitle((String) data.get("title"));
        if (data.containsKey("description")) hr.setDescription((String) data.get("description"));
        if (data.containsKey("veterinarian")) hr.setVeterinarian((String) data.get("veterinarian"));
        if (data.containsKey("notes")) hr.setNotes((String) data.get("notes"));
        String recordDate = (String) data.get("record_date");
        if (recordDate != null && !recordDate.isEmpty()) hr.setRecordDate(LocalDate.parse(recordDate));
        String nextDueDate = (String) data.get("next_due_date");
        if (nextDueDate != null && !nextDueDate.isEmpty()) hr.setNextDueDate(LocalDate.parse(nextDueDate));

        healthRecordRepository.save(hr);
        return ApiResponse.success("更新成功", Map.of("record", hr.toDict()));
    }

    @Transactional
    public ApiResponse<Void> deleteHealthRecord(HealthRecord hr) {
        healthRecordRepository.delete(hr);
        return ApiResponse.success("删除成功");
    }
}
