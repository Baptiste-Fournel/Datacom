package com.datacom.domain.product;

public class ValidationNotAllowedException extends RuntimeException {

    public ValidationNotAllowedException() {
        super("Only a VALIDATOR can validate a product");
    }
}
