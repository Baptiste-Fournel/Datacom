package com.datacom.web;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.datacom.domain.product.Product;
import com.datacom.domain.product.ProductRepository;
import java.time.Instant;
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

    private static final Instant DATE = Instant.parse("2026-07-27T10:00:00Z");

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
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING_VALIDATION"));
    }

    @Test
    void shouldReturnIncompleteProblem_whenSubmittingBeforeFinalStep() throws Exception {
        // Given
        Long id = productRepository.save(Product.createDraft(1L, DATE)).id();

        // When
        mockMvc.perform(post("/api/products/" + id + "/submit").with(csrf()))
                // Then
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.code").value("INCOMPLETE_PRODUCT"));
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
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("NOT_EDITABLE"));
    }

    @Test
    void shouldReturnNotFoundProblem_whenSubmittingUnknownProduct() throws Exception {
        // Then
        mockMvc.perform(post("/api/products/999999/submit").with(csrf()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    private Long persistedDraftAtFinalStep() {
        Product product = Product.createDraft(1L, DATE);
        product.advanceToNextStep(DATE);
        product.advanceToNextStep(DATE);
        product.advanceToNextStep(DATE);
        return productRepository.save(product).id();
    }
}
