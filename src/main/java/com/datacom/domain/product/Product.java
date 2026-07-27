package com.datacom.domain.product;

import com.datacom.domain.user.Role;
import com.datacom.domain.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "products")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "created_by", nullable = false, updatable = false)
    private long createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProductStatus status;

    @Column(name = "current_step", nullable = false)
    private WorkflowStep currentStep;

    private String name;

    private String reference;

    private String description;

    private String category;

    private String subcategory;

    private String manufacturer;

    private String country;

    private String lot;

    private String certification;

    @Column(name = "validation_comment")
    private String validationComment;

    protected Product() {
    }

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

    public void updateIdentification(String name, String reference, String description, Instant at) {
        requireEditable();
        this.name = name;
        this.reference = reference;
        this.description = description;
        touch(at);
    }

    public void updateClassification(String category, String subcategory, String manufacturer, String country,
            Instant at) {
        requireEditable();
        this.category = category;
        this.subcategory = subcategory;
        this.manufacturer = manufacturer;
        this.country = country;
        touch(at);
    }

    public void updateCertification(String lot, String certification, String validationComment, Instant at) {
        requireEditable();
        this.lot = lot;
        this.certification = certification;
        this.validationComment = validationComment;
        touch(at);
    }

    public void advanceToNextStep(Instant at) {
        requireEditable();
        this.currentStep = currentStep.next();
        touch(at);
    }

    public void submitForValidation(Instant at) {
        if (status != ProductStatus.DRAFT) {
            throw new IllegalStateException("Only a draft can be submitted for validation");
        }
        if (!currentStep.isFinal()) {
            throw new IncompleteProductException(currentStep);
        }
        this.status = ProductStatus.PENDING_VALIDATION;
        touch(at);
    }

    public void validate(User validator, Instant at) {
        Objects.requireNonNull(validator, "A validation requires a validator");
        if (!validator.hasRole(Role.VALIDATOR)) {
            throw new ValidationNotAllowedException();
        }
        if (status != ProductStatus.PENDING_VALIDATION) {
            throw new IllegalStateException("Only a product pending validation can be validated");
        }
        this.status = ProductStatus.VALIDATED;
        touch(at);
    }

    public boolean isEditable() {
        return status == ProductStatus.DRAFT;
    }

    private void requireEditable() {
        if (!isEditable()) {
            throw new IllegalStateException("The product is no longer editable");
        }
    }

    private void touch(Instant at) {
        this.updatedAt = at;
    }

    public Long id() {
        return id;
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

    public String name() {
        return name;
    }

    public String reference() {
        return reference;
    }

    public String description() {
        return description;
    }

    public String category() {
        return category;
    }

    public String subcategory() {
        return subcategory;
    }

    public String manufacturer() {
        return manufacturer;
    }

    public String country() {
        return country;
    }

    public String lot() {
        return lot;
    }

    public String certification() {
        return certification;
    }

    public String validationComment() {
        return validationComment;
    }
}
