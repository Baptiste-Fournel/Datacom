package com.datacom.domain.user;

import java.util.Optional;

public interface UserRepository {

    Optional<User> findById(Long id);
}
