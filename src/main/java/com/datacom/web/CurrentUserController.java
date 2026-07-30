package com.datacom.web;

import com.datacom.application.UserService;
import com.datacom.web.dto.CurrentUser;
import java.security.Principal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CurrentUserController {

    private final UserService userService;

    public CurrentUserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/api/auth/me")
    public CurrentUser currentUser(Principal principal) {
        return CurrentUser.from(userService.requireByLogin(principal.getName()));
    }
}
