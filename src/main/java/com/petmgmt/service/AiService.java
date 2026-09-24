package com.petmgmt.service;

import com.petmgmt.ai.*;
import com.petmgmt.model.*;
import com.petmgmt.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class AiService {

    private final BreedHistoryRepository breedHistoryRepository;
    private final EmotionHistoryRepository emotionHistoryRepository;
    private final HealthRecordRepository healthRecordRepository;
    private final WeightRecordRepository weightRecordRepository;
    private final ProductRepository productRepository;

    public AiService(BreedHistoryRepository breedHistoryRepository,
                     EmotionHistoryRepository emotionHistoryRepository,
                     HealthRecordRepository healthRecordRepository,
                     WeightRecordRepository weightRecordRepository,
                     ProductRepository productRepository) {
        this.breedHistoryRepository = breedHistoryRepository;
        this.emotionHistoryRepository = emotionHistoryRepository;
        this.healthRecordRepository = healthRecordRepository;
        this.weightRecordRepository = weightRecordRepository;
        this.productRepository = productRepository;
    }

    public Map<String, Object> predictBreed(String imagePath) {
        Map<String, Object> colorInfo = BreedModel.analyzeCatColor(imagePath);
        String[] catResult = BreedModel.catFromColorOnly(colorInfo);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("breed_name", catResult[0]);
        result.put("name_zh", catResult[0]);
        result.put("confidence", 75.0);
        result.put("description", catResult[1]);
        result.put("care_tips", catResult[2]);
        return result;
    }

    @Transactional
    public BreedHistory saveBreedHistory(User user, Pet pet, String imagePath, Map<String, Object> result) {
        BreedHistory bh = new BreedHistory();
        bh.setUser(user);
        if (pet != null) bh.setPet(pet);
        bh.setImagePath(imagePath);
        bh.setRecognizedBreed((String) result.get("breed_name"));
        bh.setConfidence(((Number) result.get("confidence")).doubleValue());
        bh.setDescription((String) result.get("description"));
        bh.setCareTips((String) result.get("care_tips"));
        return breedHistoryRepository.save(bh);
    }

    public Map<String, Object> predictEmotion(String imagePath) {
        return new EmotionModel().predict(imagePath);
    }

    @Transactional
    public EmotionHistory saveEmotionHistory(User user, Pet pet, String imagePath, Map<String, Object> result) {
        EmotionHistory eh = new EmotionHistory();
        eh.setUser(user);
        eh.setPet(pet);
        eh.setImagePath(imagePath);
        eh.setEmotion((String) result.get("emotion"));
        eh.setConfidence(((Number) result.get("confidence")).doubleValue());
        eh.setComfortAdvice((String) result.get("comfort_advice"));
        return emotionHistoryRepository.save(eh);
    }

    public String generateHealthReport(Pet pet) {
        List<Map<String, Object>> healthRecords = healthRecordRepository.findByPetIdOrderByRecordDateDesc(pet.getId())
            .stream().map(HealthRecord::toDict).toList();
        List<Map<String, Object>> weightHistory = weightRecordRepository.findTop10ByPetIdOrderByRecordDateDesc(pet.getId())
            .stream().map(WeightRecord::toDict).toList();

        return PetAi.generateHealthReport(pet.getName(), pet.getSpecies(), pet.getBreed(),
            pet.getAge(), pet.getWeight() != null ? pet.getWeight().doubleValue() : null,
            pet.getGender(), healthRecords, weightHistory);
    }

    public String recommendProducts(Pet pet) {
        List<String> speciesFilter = "both".equals(pet.getSpecies()) ? List.of("dog", "cat", "both") : List.of(pet.getSpecies(), "both");
        List<Map<String, Object>> products = productRepository.findByIsActiveTrueAndTargetSpeciesIn(speciesFilter)
            .stream().map(Product::toDict).toList();

        return PetAi.recommendProducts(pet.getName(), pet.getSpecies(), pet.getBreed(),
            pet.getAge(), pet.getWeight() != null ? pet.getWeight().doubleValue() : null, products);
    }

    public String askEncyclopedia(String question) {
        return PetAi.askEncyclopedia(question);
    }
}
