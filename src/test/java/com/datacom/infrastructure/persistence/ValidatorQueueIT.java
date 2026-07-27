package com.datacom.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.datacom.domain.product.Product;
import com.datacom.domain.product.ProductRepository;
import com.datacom.domain.product.ProductStatus;
import com.datacom.domain.user.Role;
import com.datacom.domain.user.User;
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
class ValidatorQueueIT {

    private static final Instant CREATION_DATE = Instant.parse("2026-07-27T10:00:00Z");
    private static final User VALIDATOR = new User(2L, "validator", "Jane", "Doe", Role.VALIDATOR);

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @Autowired
    private ProductRepository repository;

    @Test
    void shouldContainOnlyPendingProducts_whenQueueIsFetched() {
        // Given
        Long draftId = repository.save(draft()).id();
        Long pendingId = repository.save(pending()).id();
        Long validatedId = repository.save(validated()).id();

        // When
        var queue = repository.findByStatus(ProductStatus.PENDING_VALIDATION);

        // Then
        assertThat(queue).extracting(Product::id).contains(pendingId);
        assertThat(queue).extracting(Product::id).doesNotContain(draftId, validatedId);
        assertThat(queue).allSatisfy(
                product -> assertThat(product.status()).isEqualTo(ProductStatus.PENDING_VALIDATION));
    }

    private static Product draft() {
        return Product.createDraft(1L, CREATION_DATE);
    }

    private static Product pending() {
        Product product = draft();
        product.advanceToNextStep(CREATION_DATE);
        product.advanceToNextStep(CREATION_DATE);
        product.advanceToNextStep(CREATION_DATE);
        product.submitForValidation(CREATION_DATE);
        return product;
    }

    private static Product validated() {
        Product product = pending();
        product.validate(VALIDATOR, CREATION_DATE);
        return product;
    }
}
