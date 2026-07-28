package com.datacom.web;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
import org.springframework.http.MediaType;
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
class ProductEditionApiIT {

    private static final Instant DATE = Instant.parse("2026-07-27T10:00:00Z");

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProductRepository productRepository;

    @Test
    void shouldCreateDraftForCurrentOperator_whenPosting() throws Exception {
        // When
        mockMvc.perform(post("/api/products").with(csrf()))
                // Then
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.currentStep").value(1))
                .andExpect(jsonPath("$.createdBy").value(1));
    }

    @Test
    void shouldExposeFullDetail_whenFetchingById() throws Exception {
        // Given
        Long id = persistedDraft();

        // When
        mockMvc.perform(get("/api/products/" + id))
                // Then
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Capteur thermique T-200"))
                .andExpect(jsonPath("$.reference").value("REF-T200-FR"))
                .andExpect(jsonPath("$.description").value("Capteur agroalimentaire"))
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.createdBy").value(1))
                .andExpect(jsonPath("$.createdAt").value("2026-07-27T10:00:00Z"));
    }

    @Test
    void shouldReturnNotFoundProblem_whenProductIsUnknown() throws Exception {
        // Then
        mockMvc.perform(get("/api/products/999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void shouldSaveIdentification_whenPutOnDraft() throws Exception {
        // Given
        Long id = persistedDraft();

        // When
        mockMvc.perform(put("/api/products/" + id + "/identification").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"Sonde SP-40\", \"reference\": \"REF-SP40\", \"description\": null}"))
                // Then
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Sonde SP-40"))
                .andExpect(jsonPath("$.reference").value("REF-SP40"));
    }

    @Test
    void shouldSaveClassification_whenPutOnDraft() throws Exception {
        // Given
        Long id = persistedDraft();

        // When
        mockMvc.perform(put("/api/products/" + id + "/classification").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"category\": \"Instrumentation\", \"subcategory\": \"Capteurs\","
                                + " \"manufacturer\": \"ThermoWorks SA\", \"country\": \"France\"}"))
                // Then
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.category").value("Instrumentation"))
                .andExpect(jsonPath("$.country").value("France"));
    }

    @Test
    void shouldSaveCertification_whenPutOnDraft() throws Exception {
        // Given
        Long id = persistedDraft();

        // When
        mockMvc.perform(put("/api/products/" + id + "/certification").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"lot\": \"LOT-2026-0417\", \"certification\": \"CE / RoHS\","
                                + " \"validationComment\": \"Dossier complet\"}"))
                // Then
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lot").value("LOT-2026-0417"))
                .andExpect(jsonPath("$.validationComment").value("Dossier complet"));
    }

    @Test
    void shouldAdvanceToNextStep_whenPosting() throws Exception {
        // Given
        Long id = persistedDraft();

        // When
        mockMvc.perform(post("/api/products/" + id + "/advance").with(csrf()))
                // Then
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentStep").value(2));
    }

    @Test
    void shouldReturnNotEditableProblem_whenEditingSubmittedProduct() throws Exception {
        // Given
        Long id = persistedSubmittedProduct();

        // When
        mockMvc.perform(put("/api/products/" + id + "/identification").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"n\", \"reference\": \"r\", \"description\": \"d\"}"))
                // Then
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.code").value("NOT_EDITABLE"));
    }

    @Test
    @WithMockUser(username = "ghost", roles = "OPERATOR")
    void shouldReturnUnauthenticatedProblem_whenAccountHasVanished() throws Exception {
        // When
        mockMvc.perform(post("/api/products").with(csrf()))
                // Then
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    void shouldReturnIllegalTransitionProblem_whenAdvancingBeyondFinalStep() throws Exception {
        // Given
        Long id = persistedDraftAtFinalStep();

        // When
        mockMvc.perform(post("/api/products/" + id + "/advance").with(csrf()))
                // Then
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ILLEGAL_TRANSITION"));
    }

    private Long persistedDraft() {
        Product product = Product.createDraft(1L, DATE);
        product.updateIdentification("Capteur thermique T-200", "REF-T200-FR", "Capteur agroalimentaire", DATE);
        return productRepository.save(product).id();
    }

    private Long persistedDraftAtFinalStep() {
        Product product = Product.createDraft(1L, DATE);
        product.advanceToNextStep(DATE);
        product.advanceToNextStep(DATE);
        product.advanceToNextStep(DATE);
        return productRepository.save(product).id();
    }

    private Long persistedSubmittedProduct() {
        Product product = Product.createDraft(1L, DATE);
        product.advanceToNextStep(DATE);
        product.advanceToNextStep(DATE);
        product.advanceToNextStep(DATE);
        product.submitForValidation(DATE);
        return productRepository.save(product).id();
    }
}
