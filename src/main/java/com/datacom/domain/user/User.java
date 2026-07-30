package com.datacom.domain.user;

import java.util.Objects;

public record User(long id, String login, String firstname, String lastname, Role role) {

    public User {
        if (id <= 0) {
            throw new IllegalArgumentException("A user requires a valid identifier");
        }
        if (login == null || login.isBlank()) {
            throw new IllegalArgumentException("A user requires a non-blank login");
        }
        Objects.requireNonNull(role, "A user requires a role");
    }

    public boolean hasRole(Role expected) {
        return role == expected;
    }
}
