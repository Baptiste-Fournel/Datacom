package com.datacom.domain.product;

import java.time.Instant;
import java.util.Objects;

public class Product {

    private final long createdBy;
    private final Instant createdAt;
    private final Instant updatedAt;
    private final ProductStatus status;
    private final WorkflowStep currentStep;

    private Product(long createdBy, Instant createdAt) {
        this.createdBy = createdBy;
        this.createdAt = createdAt;
        this.updatedAt = createdAt;
        this.status = ProductStatus.DRAFT;
        this.currentStep = WorkflowStep.IDENTIFICATION;
    }

    public static Product createDraft(long creatorId, Instant creationDate) {
        if (creatorId <= 0) {
            throw new IllegalArgumentException("A product requires a valid creator identifier");
        }
        Objects.requireNonNull(creationDate, "A product requires a creation date");
        return new Product(creatorId, creationDate);
    }

    public ProductStatus status() {
        return status;
    }

    public WorkflowStep currentStep() {
        return currentStep;
    }

    public long createdBy() {
        return createdBy;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }
}
