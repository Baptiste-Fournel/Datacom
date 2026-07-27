package com.datacom.domain.product;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class ProductSubmissionTest {

    private static final Instant CREATION_DATE = Instant.parse("2026-07-27T10:00:00Z");
    private static final Instant SUBMISSION_DATE = Instant.parse("2026-07-27T14:00:00Z");

    @Test
    void shouldMoveToPendingValidation_whenSubmittedAtFinalStep() {
        Product product = draftAtFinalStep();

        product.submitForValidation(SUBMISSION_DATE);

        assertThat(product.status()).isEqualTo(ProductStatus.PENDING_VALIDATION);
        assertThat(product.updatedAt()).isEqualTo(SUBMISSION_DATE);
        assertThat(product.isEditable()).isFalse();
    }

    @Test
    void shouldRejectSubmission_whenDraftIsAtFirstStep() {
        Product product = Product.createDraft(42L, CREATION_DATE);

        assertThatExceptionOfType(IncompleteProductException.class)
                .isThrownBy(() -> product.submitForValidation(SUBMISSION_DATE))
                .withMessageContaining("final step");
        assertThat(product.status()).isEqualTo(ProductStatus.DRAFT);
        assertThat(product.updatedAt()).isEqualTo(CREATION_DATE);
    }

    @Test
    void shouldRejectSubmission_whenDraftIsAtIntermediateStep() {
        Product product = Product.createDraft(42L, CREATION_DATE);
        product.advanceToNextStep(CREATION_DATE);
        product.advanceToNextStep(CREATION_DATE);

        assertThat(product.currentStep()).isEqualTo(WorkflowStep.CERTIFICATION);
        assertThatExceptionOfType(IncompleteProductException.class)
                .isThrownBy(() -> product.submitForValidation(SUBMISSION_DATE));
        assertThat(product.status()).isEqualTo(ProductStatus.DRAFT);
    }

    @Test
    void shouldRejectSubmission_whenAlreadySubmitted() {
        Product product = draftAtFinalStep();
        product.submitForValidation(SUBMISSION_DATE);
        Instant retry = Instant.parse("2026-07-27T16:00:00Z");

        assertThatIllegalStateException()
                .isThrownBy(() -> product.submitForValidation(retry))
                .withMessageContaining("draft");
        assertThat(product.status()).isEqualTo(ProductStatus.PENDING_VALIDATION);
        assertThat(product.updatedAt()).isEqualTo(SUBMISSION_DATE);
    }

    @Test
    void shouldRejectAnyEdit_whenSubmitted() {
        Product product = draftAtFinalStep();
        product.submitForValidation(SUBMISSION_DATE);
        Instant later = Instant.parse("2026-07-27T15:00:00Z");

        assertThatIllegalStateException()
                .isThrownBy(() -> product.updateIdentification("n", "r", "d", later))
                .withMessageContaining("editable");
        assertThatIllegalStateException()
                .isThrownBy(() -> product.updateClassification("c", "s", "m", "p", later))
                .withMessageContaining("editable");
        assertThatIllegalStateException()
                .isThrownBy(() -> product.updateCertification("l", "c", "v", later))
                .withMessageContaining("editable");
        assertThatIllegalStateException()
                .isThrownBy(() -> product.advanceToNextStep(later))
                .withMessageContaining("editable");
        assertThat(product.updatedAt()).isEqualTo(SUBMISSION_DATE);
    }

    private static Product draftAtFinalStep() {
        Product product = Product.createDraft(42L, CREATION_DATE);
        product.advanceToNextStep(CREATION_DATE);
        product.advanceToNextStep(CREATION_DATE);
        product.advanceToNextStep(CREATION_DATE);
        return product;
    }
}
