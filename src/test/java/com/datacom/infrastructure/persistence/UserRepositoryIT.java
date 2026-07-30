package com.datacom.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

import com.datacom.domain.user.Role;
import com.datacom.domain.user.User;
import com.datacom.domain.user.UserRepository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@Testcontainers
class UserRepositoryIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @Autowired
    private UserRepository repository;

    @Test
    void shouldExposeSeededOperator_whenFetchedById() {
        // When
        User operator = repository.findById(1L).orElseThrow();

        // Then
        assertAll(
                () -> assertThat(operator.login()).isEqualTo("operator"),
                () -> assertThat(operator.role()).isEqualTo(Role.OPERATOR),
                () -> assertThat(operator.hasRole(Role.VALIDATOR)).isFalse());
    }

    @Test
    void shouldExposeSeededValidator_whenFetchedById() {
        // When
        User validator = repository.findById(2L).orElseThrow();

        // Then
        assertAll(
                () -> assertThat(validator.login()).isEqualTo("validator"),
                () -> assertThat(validator.hasRole(Role.VALIDATOR)).isTrue());
    }
}
