package com.example.authservice.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class OAuthLoginResponseDTO {
    private boolean success;
    private String accessToken;
    private String refreshToken;
    private boolean needsLinking; // 이메일 중복된 경우
    private String message;
    private UserInfoDTO user;
}
