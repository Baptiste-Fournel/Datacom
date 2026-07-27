package com.datacom.domain.product;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import com.datacom.domain.user.Role;
import com.datacom.domain.user.User;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class ProductValidationTest {

    private static final Instant CREATION_DATE = Instant.parse("2026-07-27T10:00:00Z");
    private static final Instant SUBMISSION_DATE = Instant.parse("2026-07-27T14:00:00Z");
    private static final Instant VALIDATION_DATE = Instant.parse("2026-07-28T09:00:00Z");
    private static final User VALIDATOR = new User(7L, "validator", "Jane", "Doe", Role.VALIDATOR);
    private static final User OPERATOR = new User(1L, "operator", "John", "Doe", Role.OPERATOR);

    @Test
    void shouldMoveToValidated_whenValidatorValidatesPendingProduct() {
        Product product = pendingProduct();

        product.validate(VALIDATOR, VALIDATION_DATE);

        assertThat(product.status()).isEqualTo(ProductStatus.VALIDATED);
        assertThat(product.updatedAt()).isEqualTo(VALIDATION_DATE);
    }

    @Test
    void shouldRejectValidation_whenUserIsNotValidator() {
        Product product = pendingProduct();

        assertThatExceptionOfType(ValidationNotAllowedException.class)
                .isThrownBy(() -> product.validate(OPERATOR, VALIDATION_DATE))
                .withMessageContaining("VALIDATOR");
        assertThat(product.status()).isEqualTo(ProductStatus.PENDING_VALIDATION);
        assertThat(product.updatedAt()).isEqualTo(SUBMISSION_DATE);
    }

    @Test
    void shouldRejectValidation_whenProductIsStillDraft() {
        Product product = Product.createDraft(1L, CREATION_DATE);

        assertThatIllegalStateException()
                .isThrownBy(() -> product.validate(VALIDATOR, VALIDATION_DATE))
                .withMessageContaining("pending validation");
        assertThat(product.status()).isEqualTo(ProductStatus.DRAFT);
    }

    @Test
    void shouldRejectValidation_whenAlreadyValidated() {
        Product product = pendingProduct();
        product.validate(VALIDATOR, VALIDATION_DATE);
        Instant retry = Instant.parse("2026-07-28T10:00:00Z");

        assertThatIllegalStateException()
                .isThrownBy(() -> product.validate(VALIDATOR, retry))
                .withMessageContaining("pending validation");
        assertThat(product.status()).isEqualTo(ProductStatus.VALIDATED);
        assertThat(product.updatedAt()).isEqualTo(VALIDATION_DATE);
    }

    @Test
    void shouldRejectValidation_whenValidatorIsMissing() {
        Product product = pendingProduct();

        assertThatNullPointerException()
                .isThrownBy(() -> product.validate(null, VALIDATION_DATE))
                .withMessageContaining("requires a validator");
    }

    @Test
    void shouldRejectValidationBeforeRevealingState_whenUserIsNotValidator() {
        Product draft = Product.createDraft(1L, CREATION_DATE);

        assertThatExceptionOfType(ValidationNotAllowedException.class)
                .isThrownBy(() -> draft.validate(OPERATOR, VALIDATION_DATE));
    }

    @Test
    void shouldStayFrozen_whenValidated() {
        Product product = pendingProduct();
        product.validate(VALIDATOR, VALIDATION_DATE);
        Instant later = Instant.parse("2026-07-28T11:00:00Z");

        assertThat(product.isEditable()).isFalse();
        assertThatIllegalStateException()
                .isThrownBy(() -> product.updateIdentification("n", "r", "d", later))
                .withMessageContaining("editable");
        assertThatIllegalStateException()
                .isThrownBy(() -> product.advanceToNextStep(later))
                .withMessageContaining("editable");
        assertThatIllegalStateException()
                .isThrownBy(() -> product.submitForValidation(later))
                .withMessageContaining("draft");
        assertThat(product.status()).isEqualTo(ProductStatus.VALIDATED);
        assertThat(product.updatedAt()).isEqualTo(VALIDATION_DATE);
    }

    private static Product pendingProduct() {
        Product product = Product.createDraft(1L, CREATION_DATE);
        product.advanceToNextStep(CREATION_DATE);
        product.advanceToNextStep(CREATION_DATE);
        product.advanceToNextStep(CREATION_DATE);
        product.submitForValidation(SUBMISSION_DATE);
        return product;
    }
}
