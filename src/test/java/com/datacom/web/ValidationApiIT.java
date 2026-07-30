package com.datacom.web;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.datacom.domain.product.ProductRepository;
import com.datacom.testsupport.ProductFixtures;
import org.hamcrest.Matchers;
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
@WithMockUser(username = "validator", roles = "VALIDATOR")
class ValidationApiIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProductRepository productRepository;

    @Test
    void shouldListOnlyPendingProducts_whenFetchingQueue() throws Exception {
        // Given
        Long draftId = productRepository.save(ProductFixtures.draft()).id();
        Long pendingId = productRepository.save(ProductFixtures.submitted()).id();
        Long validatedId = productRepository.save(ProductFixtures.validated()).id();

        // When
        mockMvc.perform(get("/api/validation/queue"))
                // Then
                .andExpectAll(
                        status().isOk(),
                        jsonPath("$[*].id", Matchers.hasItem(pendingId.intValue())),
                        jsonPath("$[*].id", Matchers.not(Matchers.hasItem(draftId.intValue()))),
                        jsonPath("$[*].id", Matchers.not(Matchers.hasItem(validatedId.intValue()))),
                        jsonPath("$[*].status", Matchers.everyItem(Matchers.is("PENDING_VALIDATION"))));
    }

    @Test
    void shouldExposePendingProductDetail_whenFetchingById() throws Exception {
        // Given
        Long id = productRepository.save(ProductFixtures.submitted()).id();

        // When
        mockMvc.perform(get("/api/validation/products/" + id))
                // Then
                .andExpectAll(
                        status().isOk(),
                        jsonPath("$.id").value(id),
                        jsonPath("$.status").value("PENDING_VALIDATION"),
                        jsonPath("$.currentStep").value(4));
    }

    @Test
    void shouldForbidDetail_whenProductIsNotPending() throws Exception {
        // Given
        Long draftId = productRepository.save(ProductFixtures.draft()).id();

        // When
        mockMvc.perform(get("/api/validation/products/" + draftId))
                // Then
                .andExpectAll(
                        status().isForbidden(),
                        jsonPath("$.status").value(403),
                        jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void shouldReturnNotFoundProblem_whenProductIsUnknown() throws Exception {
        // Then
        mockMvc.perform(get("/api/validation/products/999999"))
                .andExpectAll(
                        status().isNotFound(),
                        jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void shouldMoveToValidated_whenValidatingPendingProduct() throws Exception {
        // Given
        Long id = productRepository.save(ProductFixtures.submitted()).id();

        // When
        mockMvc.perform(post("/api/validation/products/" + id + "/validate").with(csrf()))
                // Then
                .andExpectAll(
                        status().isOk(),
                        jsonPath("$.status").value("VALIDATED"));
    }

    @Test
    @WithMockUser(username = "operator", roles = "VALIDATOR")
    void shouldForbidValidation_whenSessionRoleDriftsFromDatabase() throws Exception {
        // Given
        Long id = productRepository.save(ProductFixtures.submitted()).id();

        // When
        mockMvc.perform(post("/api/validation/products/" + id + "/validate").with(csrf()))
                // Then
                .andExpectAll(
                        status().isForbidden(),
                        jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    @WithMockUser(username = "ghost", roles = "VALIDATOR")
    void shouldReturnUnauthenticatedProblem_whenValidatorAccountHasVanished() throws Exception {
        // Given
        Long id = productRepository.save(ProductFixtures.submitted()).id();

        // When
        mockMvc.perform(post("/api/validation/products/" + id + "/validate").with(csrf()))
                // Then
                .andExpectAll(
                        status().isUnauthorized(),
                        jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    void shouldForbidValidation_whenProductIsNotPending() throws Exception {
        // Given
        Long draftId = productRepository.save(ProductFixtures.draft()).id();

        // When
        mockMvc.perform(post("/api/validation/products/" + draftId + "/validate").with(csrf()))
                // Then
                .andExpectAll(
                        status().isForbidden(),
                        jsonPath("$.code").value("FORBIDDEN"));
    }
}
