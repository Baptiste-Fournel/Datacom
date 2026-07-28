package com.datacom.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.datacom.domain.product.Product;
import com.datacom.domain.product.ProductRepository;
import com.datacom.domain.product.ProductStatus;
import com.datacom.domain.product.WorkflowStep;
import com.datacom.testsupport.ProductFixtures;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@Testcontainers
class ProductPersistenceIT {

    private static final Instant CREATION_DATE = Instant.parse("2026-07-27T10:00:00Z");
    private static final Instant SUBMISSION_DATE = Instant.parse("2026-07-27T14:00:00Z");
    private static final Instant VALIDATION_DATE = Instant.parse("2026-07-28T09:00:00Z");

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @Autowired
    private ProductRepository repository;

    @Test
    void shouldReloadDraftUnchanged_whenSavedAndFetchedById() {
        // Given
        Product draft = Product.createDraft(1L, CREATION_DATE);
        draft.updateIdentification("Capteur thermique T-200", "REF-T200-FR", "Capteur agroalimentaire",
                CREATION_DATE);

        // When
        Long id = repository.save(draft).id();
        Product reloaded = repository.findById(id).orElseThrow();

        // Then
        assertThat(reloaded.status()).isEqualTo(ProductStatus.DRAFT);
        assertThat(reloaded.currentStep()).isEqualTo(WorkflowStep.IDENTIFICATION);
        assertThat(reloaded.name()).isEqualTo("Capteur thermique T-200");
        assertThat(reloaded.reference()).isEqualTo("REF-T200-FR");
        assertThat(reloaded.description()).isEqualTo("Capteur agroalimentaire");
        assertThat(reloaded.createdBy()).isEqualTo(1L);
        assertThat(reloaded.createdAt()).isEqualTo(CREATION_DATE);
        assertThat(reloaded.updatedAt()).isEqualTo(CREATION_DATE);
    }

    @Test
    void shouldPersistStatusAndStepConversions_whenLifecycleAdvances() {
        // Given
        Product product = Product.createDraft(1L, CREATION_DATE);
        product.advanceToNextStep(CREATION_DATE);
        product.advanceToNextStep(CREATION_DATE);
        product.advanceToNextStep(CREATION_DATE);
        product.submitForValidation(SUBMISSION_DATE);

        // When
        Long id = repository.save(product).id();
        Product reloaded = repository.findById(id).orElseThrow();

        // Then
        assertThat(reloaded.status()).isEqualTo(ProductStatus.PENDING_VALIDATION);
        assertThat(reloaded.currentStep()).isEqualTo(WorkflowStep.SUMMARY);
        assertThat(reloaded.isEditable()).isFalse();
    }

    @Test
    void shouldReloadEveryField_whenFullLifecycleReachesValidation() {
        // Given
        Product product = Product.createDraft(1L, CREATION_DATE);
        product.updateIdentification("Module de charge MC-9", "REF-MC9-DE", "Module industriel", CREATION_DATE);
        product.advanceToNextStep(CREATION_DATE);
        product.updateClassification("Electronique", "Alimentation", "PowerCell GmbH", "Allemagne", CREATION_DATE);
        product.advanceToNextStep(CREATION_DATE);
        product.updateCertification("LOT-2026-0389", "CE / IEC 62133", "Dossier fournisseur complet", CREATION_DATE);
        product.advanceToNextStep(CREATION_DATE);
        product.submitForValidation(SUBMISSION_DATE);
        product.validate(ProductFixtures.SEEDED_VALIDATOR, VALIDATION_DATE);

        // When
        Long id = repository.save(product).id();
        Product reloaded = repository.findById(id).orElseThrow();

        // Then
        assertThat(reloaded.status()).isEqualTo(ProductStatus.VALIDATED);
        assertThat(reloaded.currentStep()).isEqualTo(WorkflowStep.SUMMARY);
        assertThat(reloaded.category()).isEqualTo("Electronique");
        assertThat(reloaded.subcategory()).isEqualTo("Alimentation");
        assertThat(reloaded.manufacturer()).isEqualTo("PowerCell GmbH");
        assertThat(reloaded.country()).isEqualTo("Allemagne");
        assertThat(reloaded.lot()).isEqualTo("LOT-2026-0389");
        assertThat(reloaded.certification()).isEqualTo("CE / IEC 62133");
        assertThat(reloaded.validationComment()).isEqualTo("Dossier fournisseur complet");
        assertThat(reloaded.updatedAt()).isEqualTo(VALIDATION_DATE);
    }
}
