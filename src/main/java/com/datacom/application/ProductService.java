package com.datacom.application;

import com.datacom.domain.product.Product;
import com.datacom.domain.product.ProductRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductService {

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Transactional(readOnly = true)
    public List<Product> listProducts() {
        return productRepository.findAllByOrderByIdDesc();
    }
}
