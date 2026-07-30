package com.datacom.web;

import com.datacom.application.ProductService;
import com.datacom.web.dto.CertificationUpdate;
import com.datacom.web.dto.ClassificationUpdate;
import com.datacom.web.dto.IdentificationUpdate;
import com.datacom.web.dto.ProductDetail;
import com.datacom.web.dto.ProductSummary;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping("/api/products")
    public List<ProductSummary> listProducts() {
        return productService.listProducts().stream().map(ProductSummary::from).toList();
    }

    @PostMapping("/api/products")
    @ResponseStatus(HttpStatus.CREATED)
    public ProductDetail createDraft(Principal principal) {
        return ProductDetail.from(productService.createDraft(principal.getName()));
    }

    @GetMapping("/api/products/{id}")
    public ProductDetail productDetail(@PathVariable Long id) {
        return ProductDetail.from(productService.loadProduct(id));
    }

    @PutMapping("/api/products/{id}/identification")
    public ProductDetail saveIdentification(@PathVariable Long id, @Valid @RequestBody IdentificationUpdate update) {
        return ProductDetail.from(
                productService.editIdentification(id, update.name(), update.reference(), update.description()));
    }

    @PutMapping("/api/products/{id}/classification")
    public ProductDetail saveClassification(@PathVariable Long id, @Valid @RequestBody ClassificationUpdate update) {
        return ProductDetail.from(productService.editClassification(id, update.category(), update.subcategory(),
                update.manufacturer(), update.country()));
    }

    @PutMapping("/api/products/{id}/certification")
    public ProductDetail saveCertification(@PathVariable Long id, @Valid @RequestBody CertificationUpdate update) {
        return ProductDetail.from(productService.editCertification(id, update.lot(), update.certification(),
                update.validationComment()));
    }

    @PostMapping("/api/products/{id}/advance")
    public ProductDetail advance(@PathVariable Long id) {
        return ProductDetail.from(productService.advance(id));
    }

    @PostMapping("/api/products/{id}/submit")
    public ProductDetail submit(@PathVariable Long id) {
        return ProductDetail.from(productService.submit(id));
    }
}
