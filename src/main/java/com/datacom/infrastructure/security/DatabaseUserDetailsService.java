package com.datacom.infrastructure.security;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class DatabaseUserDetailsService implements UserDetailsService {

    private static final String ACCOUNT_QUERY =
            "SELECT login, password_hash, role FROM users WHERE login = ?";

    private final JdbcTemplate jdbc;

    public DatabaseUserDetailsService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public UserDetails loadUserByUsername(String username) {
        try {
            return jdbc.queryForObject(ACCOUNT_QUERY, (row, index) -> User.builder()
                    .username(row.getString("login"))
                    .password(row.getString("password_hash"))
                    .roles(row.getString("role"))
                    .build(), username);
        } catch (EmptyResultDataAccessException e) {
            throw new UsernameNotFoundException("Unknown login", e);
        }
    }
}
