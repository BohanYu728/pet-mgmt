package com.petmgmt.controller;

import com.petmgmt.model.*;
import com.petmgmt.repository.*;
import com.petmgmt.service.*;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class BusinessBoundaryTest {
    @Test void breedCannotAttachToAnotherUsersPet() {
        UserRepository users = mock(UserRepository.class);
        PetRepository pets = mock(PetRepository.class);
        AiService service = mock(AiService.class);
        when(users.findById(1L)).thenReturn(Optional.of(User.builder().id(1L).build()));
        Pet pet = new Pet(); pet.setId(2L); pet.setOwner(User.builder().id(3L).build());
        when(pets.findById(2L)).thenReturn(Optional.of(pet));
        var controller = new AiController(service, users, pets, mock(BreedHistoryRepository.class), mock(EmotionHistoryRepository.class));
        var response = controller.recognizeBreed(new UsernamePasswordAuthenticationToken(1L, null), new MockMultipartFile("image", new byte[0]), 2L);
        assertEquals(403, response.getStatusCode().value());
        verifyNoInteractions(service);
    }

    @Test void hospitalReviewRejectsOutOfRangeRating() {
        UserRepository users = mock(UserRepository.class);
        HospitalRepository hospitals = mock(HospitalRepository.class);
        ReviewRepository reviews = mock(ReviewRepository.class);
        when(users.findById(1L)).thenReturn(Optional.of(User.builder().id(1L).build()));
        Hospital hospital = new Hospital(); hospital.setId(1L);
        when(hospitals.findById(1L)).thenReturn(Optional.of(hospital));
        var controller = new HospitalController(mock(HospitalService.class), hospitals, reviews, users);
        assertEquals(400, controller.addReview(new UsernamePasswordAuthenticationToken(1L, null), 1L, Map.of("rating", 9)).getStatusCode().value());
        verify(reviews, never()).save(any());
    }
}
