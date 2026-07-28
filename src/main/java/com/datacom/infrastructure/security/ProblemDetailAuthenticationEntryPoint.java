package com.datacom.infrastructure.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

@Component
public class ProblemDetailAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ProblemDetailResponder responder;

    public ProblemDetailAuthenticationEntryPoint(ProblemDetailResponder responder) {
        this.responder = responder;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
            AuthenticationException authenticationException) throws IOException {
        responder.respond(response, HttpStatus.UNAUTHORIZED, "UNAUTHENTICATED", "Authentication is required");
    }
}
