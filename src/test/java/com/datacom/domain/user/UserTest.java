package com.datacom.domain.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.junit.jupiter.api.Assertions.assertAll;

import org.junit.jupiter.api.Test;

class UserTest {

    @Test
    void shouldExposeIdentityAndRole_whenCreated() {
        // Arrange
        User user = new User(7L, "validator", "Jane", "Doe", Role.VALIDATOR);

        // Assert
        assertAll(
                () -> assertThat(user.id()).isEqualTo(7L),
                () -> assertThat(user.login()).isEqualTo("validator"),
                () -> assertThat(user.firstname()).isEqualTo("Jane"),
                () -> assertThat(user.lastname()).isEqualTo("Doe"),
                () -> assertThat(user.role()).isEqualTo(Role.VALIDATOR));
    }

    @Test
    void shouldConfirmRole_whenItMatches() {
        // Arrange
        User operator = new User(1L, "operator", "John", "Doe", Role.OPERATOR);

        // Assert
        assertThat(operator.hasRole(Role.OPERATOR)).isTrue();
    }

    @Test
    void shouldDenyRole_whenItDiffers() {
        // Arrange
        User operator = new User(1L, "operator", "John", "Doe", Role.OPERATOR);

        // Assert
        assertThat(operator.hasRole(Role.VALIDATOR)).isFalse();
    }

    @Test
    void shouldRejectCreation_whenIdIsInvalid() {
        // Assert
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new User(0L, "operator", "John", "Doe", Role.OPERATOR))
                .withMessageContaining("identifier");
    }

    @Test
    void shouldRejectCreation_whenLoginIsBlank() {
        // Assert
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new User(1L, "  ", "John", "Doe", Role.OPERATOR))
                .withMessageContaining("login");
    }

    @Test
    void shouldRejectCreation_whenLoginIsNull() {
        // Assert
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new User(1L, null, "John", "Doe", Role.OPERATOR))
                .withMessageContaining("login");
    }

    @Test
    void shouldRejectCreation_whenRoleIsMissing() {
        // Assert
        assertThatNullPointerException()
                .isThrownBy(() -> new User(1L, "operator", "John", "Doe", null))
                .withMessageContaining("role");
    }
}
