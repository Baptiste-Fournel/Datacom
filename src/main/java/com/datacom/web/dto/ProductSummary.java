package com.datacom.web.dto;

import com.datacom.domain.product.Product;
import com.datacom.domain.product.ProductStatus;

public record ProductSummary(Long id, String name, String reference, ProductStatus status, int currentStep) {

    public static ProductSummary from(Product product) {
        return new ProductSummary(product.id(), product.name(), product.reference(), product.status(),
                product.currentStep().number());
    }
}
