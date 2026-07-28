package com.datacom.application;

public class ProductNotPendingException extends RuntimeException {

    public ProductNotPendingException() {
        super("Access is denied");
    }
}
