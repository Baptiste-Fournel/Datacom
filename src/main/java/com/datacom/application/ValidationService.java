package com.datacom.application;

import com.datacom.domain.product.Product;
import com.datacom.domain.product.ProductRepository;
import com.datacom.domain.product.ProductStatus;
import com.datacom.domain.user.User;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ValidationService {

    private final ProductRepository productRepository;
    private final UserService userService;

    public ValidationService(ProductRepository productRepository, UserService userService) {
        this.productRepository = productRepository;
        this.userService = userService;
    }

    @Transactional(readOnly = true)
    public List<Product> pendingProducts() {
        return productRepository.findByStatus(ProductStatus.PENDING_VALIDATION);
    }

    @Transactional(readOnly = true)
    public Product loadPendingProduct(Long id) {
        Product product = productRepository.findById(id).orElseThrow(() -> new ProductNotFoundException(id));
        if (product.status() != ProductStatus.PENDING_VALIDATION) {
            throw new ProductNotPendingException();
        }
        return product;
    }

    @Transactional
    public Product validateProduct(Long id, String validatorLogin) {
        Product product = loadPendingProduct(id);
        User validator = userService.requireByLogin(validatorLogin);
        product.validate(validator, Instant.now());
        return productRepository.save(product);
    }
}
