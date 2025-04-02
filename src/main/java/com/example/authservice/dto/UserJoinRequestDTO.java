package com.example.authservice.dto;

import com.example.authservice.model.User;
import com.example.authservice.type.Role;
import lombok.Getter;
import lombok.ToString;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@Getter
@ToString
public class UserJoinRequestDTO {
    private String email;
    private String password;
    private String nickname;
    private Role role;

    public User toUser(BCryptPasswordEncoder bCryptPasswordEncoder) {
        return User.builder()
                .email(email)
                .password(bCryptPasswordEncoder.encode(password))
                .nickname(nickname)
                .role(role)
                .build();
    }

}
