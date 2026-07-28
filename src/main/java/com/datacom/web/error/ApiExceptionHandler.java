package com.datacom.web.error;

import com.datacom.application.ProductNotFoundException;
import com.datacom.application.UnknownAccountException;
import com.datacom.domain.product.IllegalTransitionException;
import com.datacom.domain.product.IncompleteProductException;
import com.datacom.domain.product.NotEditableException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(AuthenticationException.class)
    public ProblemDetail onAuthenticationFailure(AuthenticationException exception) {
        return problem(HttpStatus.UNAUTHORIZED, "UNAUTHENTICATED", "Invalid credentials");
    }

    @ExceptionHandler(ProductNotFoundException.class)
    public ProblemDetail onProductNotFound(ProductNotFoundException exception) {
        return problem(HttpStatus.NOT_FOUND, "NOT_FOUND", exception.getMessage());
    }

    @ExceptionHandler(NotEditableException.class)
    public ProblemDetail onNotEditable(NotEditableException exception) {
        return problem(HttpStatus.CONFLICT, "NOT_EDITABLE", exception.getMessage());
    }

    @ExceptionHandler(IncompleteProductException.class)
    public ProblemDetail onIncompleteProduct(IncompleteProductException exception) {
        return problem(HttpStatus.CONFLICT, "INCOMPLETE_PRODUCT", exception.getMessage());
    }

    @ExceptionHandler(IllegalTransitionException.class)
    public ProblemDetail onIllegalTransition(IllegalTransitionException exception) {
        return problem(HttpStatus.CONFLICT, "ILLEGAL_TRANSITION", exception.getMessage());
    }

    @ExceptionHandler(UnknownAccountException.class)
    public ProblemDetail onUnknownAccount(UnknownAccountException exception) {
        return problem(HttpStatus.UNAUTHORIZED, "UNAUTHENTICATED", exception.getMessage());
    }

    private static ProblemDetail problem(HttpStatus status, String code, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setProperty("code", code);
        return problem;
    }
}
