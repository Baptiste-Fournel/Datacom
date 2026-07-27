package com.datacom;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@Testcontainers
class ApplicationBootIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void shouldApplyAllMigrations_whenApplicationStarts() {
        Integer migrations = jdbc.queryForObject(
                "SELECT count(*) FROM flyway_schema_history WHERE success = true", Integer.class);

        assertThat(migrations).isGreaterThanOrEqualTo(2);
    }

    @Test
    void shouldSeedUsersWithHashedPasswords_whenMigrationsRun() {
        Integer accounts = jdbc.queryForObject(
                "SELECT count(*) FROM users WHERE password_hash LIKE '$2%'", Integer.class);

        assertThat(accounts).isEqualTo(2);
    }
}
