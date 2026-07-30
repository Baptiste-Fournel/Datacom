package com.datacom.web;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.datacom.domain.product.Product;
import com.datacom.domain.product.ProductRepository;
import com.datacom.testsupport.ProductFixtures;
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

    private static final Instant DATE = ProductFixtures.DATE;

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
                .andExpectAll(
                        status().isCreated(),
                        jsonPath("$.id").isNumber(),
                        jsonPath("$.status").value("DRAFT"),
                        jsonPath("$.currentStep").value(1),
                        jsonPath("$.createdBy").value(1));
    }

    @Test
    void shouldExposeFullDetail_whenFetchingById() throws Exception {
        // Given
        Long id = persistedDraft();

        // When
        mockMvc.perform(get("/api/products/" + id))
                // Then
                .andExpectAll(
                        status().isOk(),
                        jsonPath("$.name").value("Capteur thermique T-200"),
                        jsonPath("$.reference").value("REF-T200-FR"),
                        jsonPath("$.description").value("Capteur agroalimentaire"),
                        jsonPath("$.status").value("DRAFT"),
                        jsonPath("$.createdBy").value(1),
                        jsonPath("$.createdAt").value("2026-07-27T10:00:00Z"));
    }

    @Test
    void shouldReturnNotFoundProblem_whenProductIsUnknown() throws Exception {
        // Then
        mockMvc.perform(get("/api/products/999999"))
                .andExpectAll(
                        status().isNotFound(),
                        jsonPath("$.status").value(404),
                        jsonPath("$.code").value("NOT_FOUND"));
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
                .andExpectAll(
                        status().isOk(),
                        jsonPath("$.name").value("Sonde SP-40"),
                        jsonPath("$.reference").value("REF-SP40"));
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
                .andExpectAll(
                        status().isOk(),
                        jsonPath("$.category").value("Instrumentation"),
                        jsonPath("$.country").value("France"));
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
                .andExpectAll(
                        status().isOk(),
                        jsonPath("$.lot").value("LOT-2026-0417"),
                        jsonPath("$.validationComment").value("Dossier complet"));
    }

    @Test
    void shouldAdvanceToNextStep_whenPosting() throws Exception {
        // Given
        Long id = persistedDraft();

        // When
        mockMvc.perform(post("/api/products/" + id + "/advance").with(csrf()))
                // Then
                .andExpectAll(
                        status().isOk(),
                        jsonPath("$.currentStep").value(2));
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
                .andExpectAll(
                        status().isConflict(),
                        jsonPath("$.status").value(409),
                        jsonPath("$.code").value("NOT_EDITABLE"));
    }

    @Test
    @WithMockUser(username = "ghost", roles = "OPERATOR")
    void shouldReturnUnauthenticatedProblem_whenAccountHasVanished() throws Exception {
        // When
        mockMvc.perform(post("/api/products").with(csrf()))
                // Then
                .andExpectAll(
                        status().isUnauthorized(),
                        jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    void shouldReturnIllegalTransitionProblem_whenAdvancingBeyondFinalStep() throws Exception {
        // Given
        Long id = persistedDraftAtFinalStep();

        // When
        mockMvc.perform(post("/api/products/" + id + "/advance").with(csrf()))
                // Then
                .andExpectAll(
                        status().isConflict(),
                        jsonPath("$.code").value("ILLEGAL_TRANSITION"));
    }

    @Test
    void shouldReturnValidationProblem_whenNameExceedsStorableLength() throws Exception {
        // Given
        Long id = persistedDraft();
        String tooLong = "A".repeat(256);

        // When
        mockMvc.perform(put("/api/products/" + id + "/identification").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"" + tooLong + "\", \"reference\": \"r\", \"description\": \"d\"}"))
                // Then
                .andExpectAll(
                        status().isBadRequest(),
                        jsonPath("$.status").value(400),
                        jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void shouldReturnValidationProblem_whenReferenceExceedsStorableLength() throws Exception {
        // Given
        Long id = persistedDraft();
        String tooLong = "R".repeat(101);

        // When
        mockMvc.perform(put("/api/products/" + id + "/identification").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"n\", \"reference\": \"" + tooLong + "\", \"description\": \"d\"}"))
                // Then
                .andExpectAll(
                        status().isBadRequest(),
                        jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void shouldReturnValidationProblem_whenClassificationFieldExceedsStorableLength() throws Exception {
        // Given
        Long id = persistedDraft();
        String tooLong = "C".repeat(101);

        // When
        mockMvc.perform(put("/api/products/" + id + "/classification").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"category\": \"" + tooLong + "\", \"subcategory\": \"s\","
                                + " \"manufacturer\": \"m\", \"country\": \"c\"}"))
                // Then
                .andExpectAll(
                        status().isBadRequest(),
                        jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void shouldReturnValidationProblem_whenCertificationFieldExceedsStorableLength() throws Exception {
        // Given
        Long id = persistedDraft();
        String tooLong = "L".repeat(101);

        // When
        mockMvc.perform(put("/api/products/" + id + "/certification").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"lot\": \"" + tooLong + "\", \"certification\": \"c\","
                                + " \"validationComment\": \"v\"}"))
                // Then
                .andExpectAll(
                        status().isBadRequest(),
                        jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void shouldAcceptEmptyFields_whenDraftIsStillBeingFilled() throws Exception {
        // Given
        Long id = persistedDraft();

        // When
        mockMvc.perform(put("/api/products/" + id + "/identification").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"\", \"reference\": \"\", \"description\": \"\"}"))
                // Then
                .andExpect(status().isOk());
    }

    private Long persistedDraft() {
        Product product = Product.createDraft(1L, DATE);
        product.updateIdentification("Capteur thermique T-200", "REF-T200-FR", "Capteur agroalimentaire", DATE);
        return productRepository.save(product).id();
    }

    private Long persistedDraftAtFinalStep() {
        return productRepository.save(ProductFixtures.draftAtFinalStep()).id();
    }

    private Long persistedSubmittedProduct() {
        return productRepository.save(ProductFixtures.submitted()).id();
    }
}
