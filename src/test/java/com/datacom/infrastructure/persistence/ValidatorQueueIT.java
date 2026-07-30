package com.datacom.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

import com.datacom.domain.product.Product;
import com.datacom.domain.product.ProductRepository;
import com.datacom.domain.product.ProductStatus;
import com.datacom.testsupport.ProductFixtures;

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

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @Autowired
    private ProductRepository repository;

    @Test
    void shouldContainOnlyPendingProducts_whenQueueIsFetched() {
        // Given
        Long draftId = repository.save(ProductFixtures.draft()).id();
        Long pendingId = repository.save(ProductFixtures.submitted()).id();
        Long validatedId = repository.save(ProductFixtures.validated()).id();

        // When
        var queue = repository.findByStatus(ProductStatus.PENDING_VALIDATION);

        // Then
        assertAll(
                () -> assertThat(queue).extracting(Product::id).contains(pendingId),
                () -> assertThat(queue).extracting(Product::id).doesNotContain(draftId, validatedId),
                () -> assertThat(queue).allSatisfy(
                        product -> assertThat(product.status()).isEqualTo(ProductStatus.PENDING_VALIDATION)));
    }

}
