package com.datacom.domain.product;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.junit.jupiter.api.Assertions.assertAll;

import java.time.Instant;

import org.junit.jupiter.api.Test;

class ProductEditionTest {

    private static final Instant CREATION_DATE = Instant.parse("2026-07-27T10:00:00Z");
    private static final Instant UPDATE_DATE = Instant.parse("2026-07-27T11:30:00Z");

    @Test
    void shouldExposeIdentification_whenUpdatedAsDraft() {
        // Arrange
        Product product = Product.createDraft(42L, CREATION_DATE);

        // Act
        product.updateIdentification("Capteur thermique T-200", "REF-T200-FR", "Capteur agroalimentaire",
                UPDATE_DATE);

        // Assert
        assertAll(
                () -> assertThat(product.name()).isEqualTo("Capteur thermique T-200"),
                () -> assertThat(product.reference()).isEqualTo("REF-T200-FR"),
                () -> assertThat(product.description()).isEqualTo("Capteur agroalimentaire"),
                () -> assertThat(product.updatedAt()).isEqualTo(UPDATE_DATE));
    }

    @Test
    void shouldExposeClassification_whenUpdatedAsDraft() {
        // Arrange
        Product product = Product.createDraft(42L, CREATION_DATE);

        // Act
        product.updateClassification("Instrumentation", "Capteurs", "ThermoWorks SA", "France", UPDATE_DATE);

        // Assert
        assertAll(
                () -> assertThat(product.category()).isEqualTo("Instrumentation"),
                () -> assertThat(product.subcategory()).isEqualTo("Capteurs"),
                () -> assertThat(product.manufacturer()).isEqualTo("ThermoWorks SA"),
                () -> assertThat(product.country()).isEqualTo("France"),
                () -> assertThat(product.updatedAt()).isEqualTo(UPDATE_DATE));
    }

    @Test
    void shouldExposeCertification_whenUpdatedAsDraft() {
        // Arrange
        Product product = Product.createDraft(42L, CREATION_DATE);

        // Act
        product.updateCertification("LOT-2026-0417", "CE / RoHS", "Certificats fournisseur joints", UPDATE_DATE);

        // Assert
        assertAll(
                () -> assertThat(product.lot()).isEqualTo("LOT-2026-0417"),
                () -> assertThat(product.certification()).isEqualTo("CE / RoHS"),
                () -> assertThat(product.validationComment()).isEqualTo("Certificats fournisseur joints"),
                () -> assertThat(product.updatedAt()).isEqualTo(UPDATE_DATE));
    }

    @Test
    void shouldAdvanceToNextStep_whenDraft() {
        // Arrange
        Product product = Product.createDraft(42L, CREATION_DATE);

        // Act
        product.advanceToNextStep(UPDATE_DATE);

        // Assert
        assertAll(
                () -> assertThat(product.currentStep()).isEqualTo(WorkflowStep.CLASSIFICATION),
                () -> assertThat(product.updatedAt()).isEqualTo(UPDATE_DATE));
    }

    @Test
    void shouldRejectAdvanceAndKeepState_whenAtFinalStep() {
        // Arrange
        Product product = Product.createDraft(42L, CREATION_DATE);
        product.advanceToNextStep(UPDATE_DATE);
        product.advanceToNextStep(UPDATE_DATE);
        product.advanceToNextStep(UPDATE_DATE);

        // Assert
        Instant failedAttempt = Instant.parse("2026-07-27T12:00:00Z");
        assertAll(
                () -> assertThatExceptionOfType(IllegalTransitionException.class)
                        .isThrownBy(() -> product.advanceToNextStep(failedAttempt))
                        .withMessageContaining("final step"),
                () -> assertThat(product.currentStep()).isEqualTo(WorkflowStep.SUMMARY),
                () -> assertThat(product.updatedAt()).isEqualTo(UPDATE_DATE));
    }

    @Test
    void shouldBeEditable_whenDraft() {
        // Arrange
        Product product = Product.createDraft(42L, CREATION_DATE);

        // Assert
        assertThat(product.isEditable()).isTrue();
    }
}
