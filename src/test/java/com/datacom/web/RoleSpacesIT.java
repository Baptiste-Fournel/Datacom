package com.datacom.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class RoleSpacesIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @Autowired
    private MockMvc mockMvc;

    @Test
    @WithMockUser(username = "operator", roles = "OPERATOR")
    void shouldExposeOperatorIdentity_whenOperatorAsksWhoAmI() throws Exception {
        // When
        mockMvc.perform(get("/api/auth/me"))
                // Then
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.login").value("operator"))
                .andExpect(jsonPath("$.firstname").value("John"))
                .andExpect(jsonPath("$.lastname").value("Doe"))
                .andExpect(jsonPath("$.role").value("OPERATOR"));
    }

    @Test
    @WithMockUser(username = "validator", roles = "VALIDATOR")
    void shouldExposeValidatorIdentity_whenValidatorAsksWhoAmI() throws Exception {
        // When
        mockMvc.perform(get("/api/auth/me"))
                // Then
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.login").value("validator"))
                .andExpect(jsonPath("$.role").value("VALIDATOR"));
    }

    @Test
    void shouldRejectWhoAmI_whenAnonymous() throws Exception {
        // Then
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "operator", roles = "OPERATOR")
    void shouldForbidValidationSpace_whenOperatorEntersIt() throws Exception {
        // Then
        mockMvc.perform(get("/api/validation/queue"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "validator", roles = "VALIDATOR")
    void shouldForbidProductSpace_whenValidatorEntersIt() throws Exception {
        // Then
        mockMvc.perform(get("/api/products"))
                .andExpect(status().isForbidden());
    }
}
