package com.iuh.ChatAppValo.chat_authen;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import javax.security.auth.Subject;
import java.security.Principal;

/**
 * Class implement Principal để lưu thông tin người dùng hiện tại trong websocket
 */
@ToString
public class UserPrincipal implements Principal {

    private final String username;

    @Setter
    @Getter
    private String token;

    public UserPrincipal(String userId) {
        this.username = userId;
    }

    @Override
    public String getName() {
        return username;
    }
}
