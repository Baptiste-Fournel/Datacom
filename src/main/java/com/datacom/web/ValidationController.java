package com.datacom.web;

import com.datacom.application.ValidationService;
import com.datacom.web.dto.ProductDetail;
import com.datacom.web.dto.ProductSummary;
import java.security.Principal;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ValidationController {

    private final ValidationService validationService;

    public ValidationController(ValidationService validationService) {
        this.validationService = validationService;
    }

    @GetMapping("/api/validation/queue")
    public List<ProductSummary> queue() {
        return validationService.pendingProducts().stream().map(ProductSummary::from).toList();
    }

    @GetMapping("/api/validation/products/{id}")
    public ProductDetail pendingProductDetail(@PathVariable Long id) {
        return ProductDetail.from(validationService.loadPendingProduct(id));
    }

    @PostMapping("/api/validation/products/{id}/validate")
    public ProductDetail validateProduct(@PathVariable Long id, Principal principal) {
        return ProductDetail.from(validationService.validateProduct(id, principal.getName()));
    }
}
