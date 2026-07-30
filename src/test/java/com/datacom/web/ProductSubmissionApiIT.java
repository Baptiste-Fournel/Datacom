package com.datacom.web;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.datacom.domain.product.ProductRepository;
import com.datacom.testsupport.ProductFixtures;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@Transactional
@WithMockUser(username = "operator", roles = "OPERATOR")
class ProductSubmissionApiIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProductRepository productRepository;

    @Test
    void shouldMoveToPendingValidation_whenSubmittingCompleteDraft() throws Exception {
        // Given
        Long id = persistedDraftAtFinalStep();

        // When
        mockMvc.perform(post("/api/products/" + id + "/submit").with(csrf()))
                // Then
                .andExpectAll(
                        status().isOk(),
                        jsonPath("$.status").value("PENDING_VALIDATION"));
    }

    @Test
    void shouldReturnIncompleteProblem_whenSubmittingBeforeFinalStep() throws Exception {
        // Given
        Long id = productRepository.save(ProductFixtures.draft()).id();

        // When
        mockMvc.perform(post("/api/products/" + id + "/submit").with(csrf()))
                // Then
                .andExpectAll(
                        status().isConflict(),
                        jsonPath("$.status").value(409),
                        jsonPath("$.code").value("INCOMPLETE_PRODUCT"));
    }

    @Test
    void shouldReturnNotEditableProblem_whenSubmittingTwice() throws Exception {
        // Given
        Long id = persistedDraftAtFinalStep();
        mockMvc.perform(post("/api/products/" + id + "/submit").with(csrf()))
                .andExpect(status().isOk());

        // When
        mockMvc.perform(post("/api/products/" + id + "/submit").with(csrf()))
                // Then
                .andExpectAll(
                        status().isConflict(),
                        jsonPath("$.code").value("NOT_EDITABLE"));
    }

    @Test
    void shouldReturnNotFoundProblem_whenSubmittingUnknownProduct() throws Exception {
        // Then
        mockMvc.perform(post("/api/products/999999/submit").with(csrf()))
                .andExpectAll(
                        status().isNotFound(),
                        jsonPath("$.code").value("NOT_FOUND"));
    }

    private Long persistedDraftAtFinalStep() {
        return productRepository.save(ProductFixtures.draftAtFinalStep()).id();
    }
}
