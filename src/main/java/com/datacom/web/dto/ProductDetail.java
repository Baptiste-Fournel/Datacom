package com.datacom.web.dto;

import com.datacom.domain.product.Product;
import com.datacom.domain.product.ProductStatus;
import java.time.Instant;

public record ProductDetail(
        Long id,
        String name,
        String reference,
        ProductStatus status,
        int currentStep,
        String description,
        String category,
        String subcategory,
        String manufacturer,
        String country,
        String lot,
        String certification,
        String validationComment,
        long createdBy,
        Instant createdAt,
        Instant updatedAt) {

    public static ProductDetail from(Product product) {
        return new ProductDetail(
                product.id(),
                product.name(),
                product.reference(),
                product.status(),
                product.currentStep().number(),
                product.description(),
                product.category(),
                product.subcategory(),
                product.manufacturer(),
                product.country(),
                product.lot(),
                product.certification(),
                product.validationComment(),
                product.createdBy(),
                product.createdAt(),
                product.updatedAt());
    }
}
