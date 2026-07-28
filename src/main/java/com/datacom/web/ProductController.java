package com.datacom.web;

import com.datacom.application.ProductService;
import com.datacom.web.dto.ProductSummary;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
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
}
