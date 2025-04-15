package com.example.authservice.dto;

import com.example.authservice.model.User;
import com.example.authservice.type.Provider;
import com.example.authservice.type.Role;
import com.example.authservice.type.Status;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@Getter
@Setter
@ToString
@Builder
public class UserJoinRequestDTO {
    private String email;
    private String password;
    private String nickname;
    private String profileImage;
    private Role role;
    private Provider provider;
    private Status status;

    public User toUser(BCryptPasswordEncoder bCryptPasswordEncoder) {
        return User.builder()
                .email(email)
                .password(bCryptPasswordEncoder.encode(password))
                .nickname(nickname)
                .profileImage(profileImage)
                .role(role != null ? role : Role.STUDENT)
                .provider(provider != null ? provider : Provider.LOCAL)
                .status(status != null ? status : Status.ACTIVE)
                .build();
    }

}
