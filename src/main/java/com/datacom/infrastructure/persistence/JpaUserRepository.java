package com.datacom.infrastructure.persistence;

import com.datacom.domain.user.User;
import com.datacom.domain.user.UserRepository;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaUserRepository extends UserRepository, JpaRepository<User, Long> {
}
