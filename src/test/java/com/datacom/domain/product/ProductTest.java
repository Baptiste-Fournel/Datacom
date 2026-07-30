package com.datacom.domain.product;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.junit.jupiter.api.Assertions.assertAll;

import java.time.Instant;

import org.junit.jupiter.api.Test;

class ProductTest {

    private static final long CREATOR_ID = 42L;
    private static final Instant CREATION_DATE = Instant.parse("2026-07-27T10:00:00Z");

    @Test
    void shouldStartAsDraftAtFirstStep_whenCreated() {
        // Arrange
        Product product = Product.createDraft(CREATOR_ID, CREATION_DATE);

        // Assert
        assertAll(
                () -> assertThat(product.status()).isEqualTo(ProductStatus.DRAFT),
                () -> assertThat(product.currentStep()).isEqualTo(WorkflowStep.IDENTIFICATION));
    }

    @Test
    void shouldRecordCreatorAndDates_whenCreated() {
        // Arrange
        Product product = Product.createDraft(CREATOR_ID, CREATION_DATE);

        // Assert
        assertAll(
                () -> assertThat(product.createdBy()).isEqualTo(CREATOR_ID),
                () -> assertThat(product.createdAt()).isEqualTo(CREATION_DATE),
                () -> assertThat(product.updatedAt()).isEqualTo(CREATION_DATE));
    }

    @Test
    void shouldRejectCreation_whenCreatorIdIsInvalid() {
        // Assert
        assertAll(
                () -> assertThatIllegalArgumentException()
                        .isThrownBy(() -> Product.createDraft(0L, CREATION_DATE))
                        .withMessageContaining("creator"),
                () -> assertThatIllegalArgumentException()
                        .isThrownBy(() -> Product.createDraft(-1L, CREATION_DATE))
                        .withMessageContaining("creator"));
    }

    @Test
    void shouldRejectCreation_whenCreationDateIsMissing() {
        // Assert
        assertThatNullPointerException()
                .isThrownBy(() -> Product.createDraft(CREATOR_ID, null))
                .withMessageContaining("creation date");
    }
}
