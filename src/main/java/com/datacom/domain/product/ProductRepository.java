package com.datacom.domain.product;

import java.util.List;
import java.util.Optional;

public interface ProductRepository {

    Product save(Product product);

    Optional<Product> findById(Long id);

    List<Product> findByStatus(ProductStatus status);

    List<Product> findAllByOrderByIdDesc();
}
