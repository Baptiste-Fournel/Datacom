package com.datacom.web.dto;

import com.datacom.domain.user.Role;
import com.datacom.domain.user.User;

public record CurrentUser(long id, String login, String firstname, String lastname, Role role) {

    public static CurrentUser from(User user) {
        return new CurrentUser(user.id(), user.login(), user.firstname(), user.lastname(), user.role());
    }
}
