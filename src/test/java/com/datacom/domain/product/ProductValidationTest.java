package com.datacom.domain.product;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.junit.jupiter.api.Assertions.assertAll;

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
        // Arrange
        Product product = pendingProduct();

        // Act
        product.validate(VALIDATOR, VALIDATION_DATE);

        // Assert
        assertAll(
                () -> assertThat(product.status()).isEqualTo(ProductStatus.VALIDATED),
                () -> assertThat(product.updatedAt()).isEqualTo(VALIDATION_DATE));
    }

    @Test
    void shouldRejectValidation_whenUserIsNotValidator() {
        // Arrange
        Product product = pendingProduct();

        // Assert
        assertAll(
                () -> assertThatExceptionOfType(ValidationNotAllowedException.class)
                        .isThrownBy(() -> product.validate(OPERATOR, VALIDATION_DATE))
                        .withMessageContaining("VALIDATOR"),
                () -> assertThat(product.status()).isEqualTo(ProductStatus.PENDING_VALIDATION),
                () -> assertThat(product.updatedAt()).isEqualTo(SUBMISSION_DATE));
    }

    @Test
    void shouldRejectValidation_whenProductIsStillDraft() {
        // Arrange
        Product product = Product.createDraft(1L, CREATION_DATE);

        // Assert
        assertAll(
                () -> assertThatIllegalStateException()
                        .isThrownBy(() -> product.validate(VALIDATOR, VALIDATION_DATE))
                        .withMessageContaining("pending validation"),
                () -> assertThat(product.status()).isEqualTo(ProductStatus.DRAFT));
    }

    @Test
    void shouldRejectValidation_whenAlreadyValidated() {
        // Arrange
        Product product = pendingProduct();
        product.validate(VALIDATOR, VALIDATION_DATE);
        Instant retry = Instant.parse("2026-07-28T10:00:00Z");

        // Assert
        assertAll(
                () -> assertThatIllegalStateException()
                        .isThrownBy(() -> product.validate(VALIDATOR, retry))
                        .withMessageContaining("pending validation"),
                () -> assertThat(product.status()).isEqualTo(ProductStatus.VALIDATED),
                () -> assertThat(product.updatedAt()).isEqualTo(VALIDATION_DATE));
    }

    @Test
    void shouldRejectValidation_whenValidatorIsMissing() {
        // Arrange
        Product product = pendingProduct();

        // Assert
        assertThatNullPointerException()
                .isThrownBy(() -> product.validate(null, VALIDATION_DATE))
                .withMessageContaining("requires a validator");
    }

    @Test
    void shouldRejectValidationBeforeRevealingState_whenUserIsNotValidator() {
        // Arrange
        Product draft = Product.createDraft(1L, CREATION_DATE);

        // Assert
        assertThatExceptionOfType(ValidationNotAllowedException.class)
                .isThrownBy(() -> draft.validate(OPERATOR, VALIDATION_DATE));
    }

    @Test
    void shouldStayFrozen_whenValidated() {
        // Arrange
        Product product = pendingProduct();
        product.validate(VALIDATOR, VALIDATION_DATE);
        Instant later = Instant.parse("2026-07-28T11:00:00Z");

        // Assert
        assertAll(
                () -> assertThat(product.isEditable()).isFalse(),
                () -> assertThatExceptionOfType(NotEditableException.class)
                        .isThrownBy(() -> product.updateIdentification("n", "r", "d", later)),
                () -> assertThatExceptionOfType(NotEditableException.class)
                        .isThrownBy(() -> product.advanceToNextStep(later)),
                () -> assertThatExceptionOfType(NotEditableException.class)
                        .isThrownBy(() -> product.submitForValidation(later)),
                () -> assertThat(product.status()).isEqualTo(ProductStatus.VALIDATED),
                () -> assertThat(product.updatedAt()).isEqualTo(VALIDATION_DATE));
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
