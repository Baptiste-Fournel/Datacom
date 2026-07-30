package com.datacom.web;

import com.datacom.domain.user.UserRepository;
import com.datacom.web.dto.CurrentUser;
import java.security.Principal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CurrentUserController {

    private final UserRepository userRepository;

    public CurrentUserController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping("/api/auth/me")
    public CurrentUser currentUser(Principal principal) {
        return userRepository.findByLogin(principal.getName())
                .map(CurrentUser::from)
                .orElseThrow(() -> new IllegalStateException("Authenticated user has no matching record"));
    }
}
