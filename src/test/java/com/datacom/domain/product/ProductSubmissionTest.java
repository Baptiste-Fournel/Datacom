package com.datacom.domain.product;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.junit.jupiter.api.Assertions.assertAll;

import java.time.Instant;

import org.junit.jupiter.api.Test;

class ProductSubmissionTest {

    private static final Instant CREATION_DATE = Instant.parse("2026-07-27T10:00:00Z");
    private static final Instant SUBMISSION_DATE = Instant.parse("2026-07-27T14:00:00Z");

    @Test
    void shouldMoveToPendingValidation_whenSubmittedAtFinalStep() {
        // Arrange
        Product product = draftAtFinalStep();

        // Act
        product.submitForValidation(SUBMISSION_DATE);

        // Assert
        assertAll(
                () -> assertThat(product.status()).isEqualTo(ProductStatus.PENDING_VALIDATION),
                () -> assertThat(product.updatedAt()).isEqualTo(SUBMISSION_DATE),
                () -> assertThat(product.isEditable()).isFalse());
    }

    @Test
    void shouldRejectSubmission_whenDraftIsAtFirstStep() {
        // Arrange
        Product product = Product.createDraft(42L, CREATION_DATE);

        // Assert
        assertAll(
                () -> assertThatExceptionOfType(IncompleteProductException.class)
                        .isThrownBy(() -> product.submitForValidation(SUBMISSION_DATE))
                        .withMessageContaining("final step"),
                () -> assertThat(product.status()).isEqualTo(ProductStatus.DRAFT),
                () -> assertThat(product.updatedAt()).isEqualTo(CREATION_DATE));
    }

    @Test
    void shouldRejectSubmission_whenDraftIsAtIntermediateStep() {
        // Arrange
        Product product = Product.createDraft(42L, CREATION_DATE);
        product.advanceToNextStep(CREATION_DATE);
        product.advanceToNextStep(CREATION_DATE);

        // Assert
        assertAll(
                () -> assertThat(product.currentStep()).isEqualTo(WorkflowStep.CERTIFICATION),
                () -> assertThatExceptionOfType(IncompleteProductException.class)
                        .isThrownBy(() -> product.submitForValidation(SUBMISSION_DATE)),
                () -> assertThat(product.status()).isEqualTo(ProductStatus.DRAFT));
    }

    @Test
    void shouldRejectSubmission_whenAlreadySubmitted() {
        // Arrange
        Product product = draftAtFinalStep();
        product.submitForValidation(SUBMISSION_DATE);
        Instant retry = Instant.parse("2026-07-27T16:00:00Z");

        // Assert
        assertAll(
                () -> assertThatExceptionOfType(NotEditableException.class)
                        .isThrownBy(() -> product.submitForValidation(retry))
                        .withMessageContaining("editable"),
                () -> assertThat(product.status()).isEqualTo(ProductStatus.PENDING_VALIDATION),
                () -> assertThat(product.updatedAt()).isEqualTo(SUBMISSION_DATE));
    }

    @Test
    void shouldRejectAnyEdit_whenSubmitted() {
        // Arrange
        Product product = draftAtFinalStep();
        product.submitForValidation(SUBMISSION_DATE);
        Instant later = Instant.parse("2026-07-27T15:00:00Z");

        // Assert
        assertAll(
                () -> assertThatExceptionOfType(NotEditableException.class)
                        .isThrownBy(() -> product.updateIdentification("n", "r", "d", later)),
                () -> assertThatExceptionOfType(NotEditableException.class)
                        .isThrownBy(() -> product.updateClassification("c", "s", "m", "p", later)),
                () -> assertThatExceptionOfType(NotEditableException.class)
                        .isThrownBy(() -> product.updateCertification("l", "c", "v", later)),
                () -> assertThatExceptionOfType(NotEditableException.class)
                        .isThrownBy(() -> product.advanceToNextStep(later)),
                () -> assertThat(product.updatedAt()).isEqualTo(SUBMISSION_DATE));
    }

    private static Product draftAtFinalStep() {
        Product product = Product.createDraft(42L, CREATION_DATE);
        product.advanceToNextStep(CREATION_DATE);
        product.advanceToNextStep(CREATION_DATE);
        product.advanceToNextStep(CREATION_DATE);
        return product;
    }
}
