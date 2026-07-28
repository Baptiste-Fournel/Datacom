package com.datacom.application;

public class UnknownAccountException extends RuntimeException {

    public UnknownAccountException() {
        super("No account matches the authenticated session");
    }
}
