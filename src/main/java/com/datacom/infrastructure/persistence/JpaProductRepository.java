package com.datacom.infrastructure.persistence;

import com.datacom.domain.product.Product;
import com.datacom.domain.product.ProductRepository;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaProductRepository extends ProductRepository, JpaRepository<Product, Long> {
}
