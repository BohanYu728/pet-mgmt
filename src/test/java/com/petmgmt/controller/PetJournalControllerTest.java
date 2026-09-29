package com.petmgmt.controller;

import com.petmgmt.model.Pet;
import com.petmgmt.model.User;
import com.petmgmt.repository.*;
import com.petmgmt.service.HealthService;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Optional;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class PetJournalControllerTest {
    private final PetRepository pets = mock(PetRepository.class);
    private final HealthRecordRepository health = mock(HealthRecordRepository.class);
    private final ReminderRepository reminders = mock(ReminderRepository.class);
    private final WeightRecordRepository weights = mock(WeightRecordRepository.class);
    private final DailyLogRepository logs = mock(DailyLogRepository.class);
    private final PetPhotoRepository photos = mock(PetPhotoRepository.class);
    private final HealthService healthService = mock(HealthService.class);
    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(new PetJournalController(pets, health, reminders, weights, logs, photos, healthService)).build();

    private void petOwnedBy(long ownerId) {
        User owner = new User(); owner.setId(ownerId);
        Pet pet = new Pet(); pet.setId(9L); pet.setOwner(owner);
        when(pets.findById(9L)).thenReturn(Optional.of(pet));
    }

    @Test
    void anotherUsersWeightRecordsCannotBeRead() throws Exception {
        petOwnedBy(1L);
        mvc.perform(get("/api/pets/9/weights").principal(new UsernamePasswordAuthenticationToken(2L, null)))
            .andExpect(status().isForbidden());
        verifyNoInteractions(weights);
    }

    @Test
    void invalidReminderDateReturnsBadRequestAndDoesNotPersist() throws Exception {
        petOwnedBy(1L);
        mvc.perform(post("/api/pets/9/reminders")
                .principal(new UsernamePasswordAuthenticationToken(1L, null))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"疫苗\",\"reminder_date\":\"tomorrow\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").exists());
        verifyNoInteractions(reminders);
    }
}
