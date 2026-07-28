package com.datacom.application;

import com.datacom.domain.user.User;
import com.datacom.domain.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public User requireByLogin(String login) {
        return userRepository.findByLogin(login).orElseThrow(UnknownAccountException::new);
    }
}
