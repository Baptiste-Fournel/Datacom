package com.datacom.domain.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.Objects;

@Entity
@Table(name = "users")
public class User {

    @Id
    private long id;

    @Column(nullable = false, unique = true)
    private String login;

    @Column(nullable = false)
    private String firstname;

    @Column(nullable = false)
    private String lastname;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    protected User() {
    }

    public User(long id, String login, String firstname, String lastname, Role role) {
        if (id <= 0) {
            throw new IllegalArgumentException("A user requires a valid identifier");
        }
        if (login == null || login.isBlank()) {
            throw new IllegalArgumentException("A user requires a non-blank login");
        }
        Objects.requireNonNull(role, "A user requires a role");
        this.id = id;
        this.login = login;
        this.firstname = firstname;
        this.lastname = lastname;
        this.role = role;
    }

    public boolean hasRole(Role expected) {
        return role == expected;
    }

    public long id() {
        return id;
    }

    public String login() {
        return login;
    }

    public String firstname() {
        return firstname;
    }

    public String lastname() {
        return lastname;
    }

    public Role role() {
        return role;
    }
}
