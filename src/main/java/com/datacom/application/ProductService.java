package com.datacom.application;

import com.datacom.domain.product.Product;
import com.datacom.domain.product.ProductRepository;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final UserService userService;

    public ProductService(ProductRepository productRepository, UserService userService) {
        this.productRepository = productRepository;
        this.userService = userService;
    }

    @Transactional(readOnly = true)
    public List<Product> listProducts() {
        return productRepository.findAllByOrderByIdDesc();
    }

    @Transactional
    public Product createDraft(String creatorLogin) {
        long creatorId = userService.requireByLogin(creatorLogin).id();
        return productRepository.save(Product.createDraft(creatorId, Instant.now()));
    }

    @Transactional(readOnly = true)
    public Product loadProduct(Long id) {
        return productRepository.findById(id).orElseThrow(() -> new ProductNotFoundException(id));
    }

    @Transactional
    public Product editIdentification(Long id, String name, String reference, String description) {
        Product product = loadProduct(id);
        product.updateIdentification(name, reference, description, Instant.now());
        return productRepository.save(product);
    }

    @Transactional
    public Product editClassification(Long id, String category, String subcategory, String manufacturer,
            String country) {
        Product product = loadProduct(id);
        product.updateClassification(category, subcategory, manufacturer, country, Instant.now());
        return productRepository.save(product);
    }

    @Transactional
    public Product editCertification(Long id, String lot, String certification, String validationComment) {
        Product product = loadProduct(id);
        product.updateCertification(lot, certification, validationComment, Instant.now());
        return productRepository.save(product);
    }

    @Transactional
    public Product advance(Long id) {
        Product product = loadProduct(id);
        product.advanceToNextStep(Instant.now());
        return productRepository.save(product);
    }

    @Transactional
    public Product submit(Long id) {
        Product product = loadProduct(id);
        product.submitForValidation(Instant.now());
        return productRepository.save(product);
    }
}
