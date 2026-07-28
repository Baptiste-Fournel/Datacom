package com.datacom.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class AuthenticationIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldOpenAuthenticatedSession_whenCredentialsAreValid() throws Exception {
        // When
        HttpSession session = mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"login\": \"operator\", \"password\": \"operator\"}"))
                .andExpect(status().isNoContent())
                .andReturn().getRequest().getSession(false);

        // Then
        mockMvc.perform(get("/api/probe").session((MockHttpSession) session))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldRotateSessionId_whenLoggingIn() throws Exception {
        // Given
        MockHttpSession presetSession = new MockHttpSession();
        String initialId = presetSession.getId();

        // When
        HttpSession session = mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .session(presetSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"login\": \"operator\", \"password\": \"operator\"}"))
                .andExpect(status().isNoContent())
                .andReturn().getRequest().getSession(false);

        // Then
        assertThat(session.getId()).isNotEqualTo(initialId);
    }

    @Test
    void shouldAcceptMutation_whenIssuedCsrfCookieIsReplayedInHeader() throws Exception {
        // Given
        Cookie xsrfCookie = mockMvc.perform(get("/api/probe"))
                .andExpect(status().isUnauthorized())
                .andReturn().getResponse().getCookie("XSRF-TOKEN");

        // Then
        mockMvc.perform(post("/api/auth/login")
                        .cookie(xsrfCookie)
                        .header("X-XSRF-TOKEN", xsrfCookie.getValue())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"login\": \"operator\", \"password\": \"operator\"}"))
                .andExpect(status().isNoContent());
    }

    @Test
    void shouldRejectLogin_whenPasswordIsWrong() throws Exception {
        // Then
        mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"login\": \"operator\", \"password\": \"wrong\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldRejectLogin_whenUserIsUnknown() throws Exception {
        // Then
        mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"login\": \"ghost\", \"password\": \"ghost\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldRejectLogin_whenCsrfTokenIsMissing() throws Exception {
        // Then
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"login\": \"operator\", \"password\": \"operator\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldRejectAnonymousAccessAndIssueCsrfCookie_whenSessionIsAbsent() throws Exception {
        // Then
        mockMvc.perform(get("/api/probe"))
                .andExpect(status().isUnauthorized())
                .andExpect(cookie().exists("XSRF-TOKEN"));
    }

    @Test
    void shouldCloseSession_whenLoggingOut() throws Exception {
        // Given
        MockHttpSession session = (MockHttpSession) mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"login\": \"validator\", \"password\": \"validator\"}"))
                .andExpect(status().isNoContent())
                .andReturn().getRequest().getSession(false);

        // When
        mockMvc.perform(post("/api/auth/logout").with(csrf()).session(session))
                .andExpect(status().isNoContent());

        // Then
        mockMvc.perform(get("/api/probe").session(session))
                .andExpect(status().isUnauthorized());
    }
}
