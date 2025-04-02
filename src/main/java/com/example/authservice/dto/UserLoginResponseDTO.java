package com.example.authservice.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class UserLoginResponseDTO {
    private boolean loggedIn;
    private String email;
    private String nickname;
    private String accessToken;
    private String refreshToken;
}
