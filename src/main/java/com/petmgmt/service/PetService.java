package com.petmgmt.service;

import com.petmgmt.dto.ApiResponse;
import com.petmgmt.model.Pet;
import com.petmgmt.model.User;
import com.petmgmt.repository.PetRepository;
import com.petmgmt.util.FileUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@Service
public class PetService {

    private final PetRepository petRepository;

    public PetService(PetRepository petRepository) {
        this.petRepository = petRepository;
    }

    @Transactional
    public ApiResponse<Map<String, Object>> createPet(User owner, Map<String, Object> data) {
        Pet pet = new Pet();
        pet.setOwner(owner);
        pet.setName((String) data.get("name"));
        pet.setSpecies((String) data.getOrDefault("species", "dog"));
        pet.setBreed((String) data.getOrDefault("breed", ""));
        pet.setGender((String) data.getOrDefault("gender", "unknown"));
        pet.setColor((String) data.getOrDefault("color", ""));
        pet.setDescription((String) data.getOrDefault("description", ""));

        Object weightVal = data.get("weight");
        if (weightVal instanceof Number) pet.setWeight(BigDecimal.valueOf(((Number) weightVal).doubleValue()));

        String birthDate = (String) data.get("birth_date");
        if (birthDate != null && !birthDate.isEmpty()) {
            pet.setBirthDate(LocalDate.parse(birthDate, DateTimeFormatter.ISO_LOCAL_DATE));
        }
        Object ageVal = data.get("age");
        if (ageVal instanceof Number) pet.setAge(BigDecimal.valueOf(((Number) ageVal).doubleValue()));

        petRepository.save(pet);
        return ApiResponse.success("添加成功", Map.of("pet", pet.toDict()));
    }

    @Transactional
    public ApiResponse<Map<String, Object>> updatePet(Pet pet, Map<String, Object> data) {
        if (data.containsKey("name")) pet.setName((String) data.get("name"));
        if (data.containsKey("species")) pet.setSpecies((String) data.get("species"));
        if (data.containsKey("breed")) pet.setBreed((String) data.get("breed"));
        if (data.containsKey("gender")) pet.setGender((String) data.get("gender"));
        if (data.containsKey("color")) pet.setColor((String) data.get("color"));
        if (data.containsKey("description")) pet.setDescription((String) data.get("description"));

        Object weightVal = data.get("weight");
        if (weightVal instanceof Number) pet.setWeight(BigDecimal.valueOf(((Number) weightVal).doubleValue()));

        String birthDate = (String) data.get("birth_date");
        if (birthDate != null && !birthDate.isEmpty()) {
            pet.setBirthDate(LocalDate.parse(birthDate, DateTimeFormatter.ISO_LOCAL_DATE));
        }
        Object ageVal = data.get("age");
        if (ageVal instanceof Number) pet.setAge(BigDecimal.valueOf(((Number) ageVal).doubleValue()));

        petRepository.save(pet);
        return ApiResponse.success("更新成功", Map.of("pet", pet.toDict()));
    }

    @Transactional
    public ApiResponse<Void> deletePet(Pet pet) {
        if (pet.getAvatar() != null) FileUtils.deleteUpload(pet.getAvatar());
        petRepository.delete(pet);
        return ApiResponse.success("删除成功");
    }
}
