package com.petmgmt;

import com.petmgmt.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties="spring.datasource.url=jdbc:h2:mem:demo-experience;MODE=MySQL")
@AutoConfigureMockMvc
@ActiveProfiles("demo")
class DemoExperienceIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired WeightRecordRepository weights;
    @Autowired DailyLogRepository logs;
    @Autowired OrderRepository orders;
    @Autowired RentalRecordRepository bills;
    @Autowired com.petmgmt.util.DemoExperienceInitializer initializer;
    @Test void publicDemoEntryAndUsableSampleData() throws Exception {
        mvc.perform(get("/api/demo/status")).andExpect(status().isOk())
            .andExpect(jsonPath("enabled").value(true))
            .andExpect(jsonPath("accounts.length()").value(3));
        assertThat(weights.count()).isGreaterThanOrEqualTo(6);
        assertThat(logs.count()).isGreaterThanOrEqualTo(2);
        assertThat(orders.count()).isGreaterThanOrEqualTo(1);
        assertThat(bills.count()).isGreaterThanOrEqualTo(1);
        mvc.perform(get("/api/admin/users")).andExpect(status().isUnauthorized());
        long weightCount = weights.count(), orderCount = orders.count();
        initializer.seed();
        assertThat(weights.count()).isEqualTo(weightCount);
        assertThat(orders.count()).isEqualTo(orderCount);
    }
    @Test void normalEnvironmentDoesNotExposeDemoAccounts() {
        var controller = new com.petmgmt.controller.DemoController(new org.springframework.mock.env.MockEnvironment());
        assertThat(controller.status()).containsEntry("enabled", false).doesNotContainKey("accounts");
    }
}
