package com.datacom.web.error;

import com.datacom.application.ProductNotFoundException;
import com.datacom.application.ProductNotPendingException;
import com.datacom.application.UnknownAccountException;
import com.datacom.domain.product.IllegalTransitionException;
import com.datacom.domain.product.IncompleteProductException;
import com.datacom.domain.product.NotEditableException;
import com.datacom.domain.product.ValidationNotAllowedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.TypeMismatchException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger LOG = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(AuthenticationException.class)
    public ProblemDetail onAuthenticationFailure(AuthenticationException exception) {
        LOG.warn("Failed authentication attempt");
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

    @ExceptionHandler(ProductNotPendingException.class)
    public ProblemDetail onProductNotPending(ProductNotPendingException exception) {
        return problem(HttpStatus.FORBIDDEN, "FORBIDDEN", exception.getMessage());
    }

    @ExceptionHandler(ValidationNotAllowedException.class)
    public ProblemDetail onValidationNotAllowed(ValidationNotAllowedException exception) {
        return problem(HttpStatus.FORBIDDEN, "FORBIDDEN", "Access is denied");
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail onUnexpectedError(Exception exception) {
        LOG.error("Unhandled exception", exception);
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, null, "An unexpected error occurred");
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException exception,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        return ResponseEntity.status(status)
                .body(problem(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Malformed request body"));
    }

    @Override
    protected ResponseEntity<Object> handleTypeMismatch(TypeMismatchException exception, HttpHeaders headers,
            HttpStatusCode status, WebRequest request) {
        return ResponseEntity.status(status)
                .body(problem(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Invalid request parameter"));
    }

    private static ProblemDetail problem(HttpStatus status, String code, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        if (code != null) {
            problem.setProperty("code", code);
        }
        return problem;
    }
}
