package com.datacom.domain.product;

public class NotEditableException extends RuntimeException {

    public NotEditableException() {
        super("The product is no longer editable");
    }
}
