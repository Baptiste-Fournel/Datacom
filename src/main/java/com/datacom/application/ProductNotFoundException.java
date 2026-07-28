package com.datacom.application;

public class ProductNotFoundException extends RuntimeException {

    public ProductNotFoundException(Long id) {
        super("No product found with id " + id);
    }
}
