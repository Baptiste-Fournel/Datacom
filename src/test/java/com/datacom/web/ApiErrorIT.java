package com.datacom.web;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@Import(ApiErrorIT.FailingController.class)
class ApiErrorIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldReturnUnauthenticatedProblem_whenAnonymous() throws Exception {
        // Then
        mockMvc.perform(get("/api/auth/me"))
                .andExpectAll(
                        status().isUnauthorized(),
                        content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON),
                        jsonPath("$.type").value("about:blank"),
                        jsonPath("$.title").value("Unauthorized"),
                        jsonPath("$.status").value(401),
                        jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    void shouldReturnUnauthenticatedProblem_whenCredentialsAreWrong() throws Exception {
        // Then
        mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"login\": \"operator\", \"password\": \"wrong\"}"))
                .andExpectAll(
                        status().isUnauthorized(),
                        content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON),
                        jsonPath("$.type").value("about:blank"),
                        jsonPath("$.title").value("Unauthorized"),
                        jsonPath("$.status").value(401),
                        jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    @WithMockUser(username = "operator", roles = "OPERATOR")
    void shouldReturnForbiddenProblem_whenEnteringForeignSpace() throws Exception {
        // Then
        mockMvc.perform(get("/api/validation/queue"))
                .andExpectAll(
                        status().isForbidden(),
                        content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON),
                        jsonPath("$.status").value(403),
                        jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void shouldAllowConfiguredFrontOrigin_whenPreflightingLogin() throws Exception {
        // Then
        mockMvc.perform(options("/api/auth/login")
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "POST"))
                .andExpectAll(
                        status().isOk(),
                        header().string("Access-Control-Allow-Origin", "http://localhost:5173"),
                        header().string("Access-Control-Allow-Credentials", "true"));
    }

    @Test
    void shouldRejectUnknownOrigin_whenPreflighting() throws Exception {
        // Then
        mockMvc.perform(options("/api/auth/login")
                        .header("Origin", "http://evil.example")
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "operator", roles = "OPERATOR")
    void shouldReturnValidationProblem_whenBodyIsMalformed() throws Exception {
        // Then
        mockMvc.perform(put("/api/products/1/identification").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{broken"))
                .andExpectAll(
                        status().isBadRequest(),
                        jsonPath("$.status").value(400),
                        jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    @WithMockUser(username = "operator", roles = "OPERATOR")
    void shouldReturnValidationProblem_whenPathIdIsNotNumeric() throws Exception {
        // Then
        mockMvc.perform(get("/api/products/not-a-number"))
                .andExpectAll(
                        status().isBadRequest(),
                        jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    @WithMockUser(username = "operator", roles = "OPERATOR")
    void shouldHideInternalDetails_whenServerFailsUnexpectedly() throws Exception {
        // When
        mockMvc.perform(get("/api/boom"))
                // Then
                .andExpectAll(
                        status().isInternalServerError(),
                        content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON),
                        jsonPath("$.detail").value("An unexpected error occurred"),
                        jsonPath("$.code").doesNotExist(),
                        content().string(Matchers.not(Matchers.containsString("secret internal detail"))),
                        content().string(Matchers.not(Matchers.containsString("IllegalArgumentException"))));
    }

    @RestController
    static class FailingController {

        @GetMapping("/api/boom")
        public String boom() {
            throw new IllegalArgumentException("secret internal detail");
        }
    }
}
