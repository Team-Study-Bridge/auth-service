package com.example.authservice.dto;

import com.example.authservice.model.User;
import com.example.authservice.type.Provider;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class OAuthUserSessionDTO {
    private String jwtToken;
    private String refreshToken;
    private boolean needsLinking;
    private User user;  // 기존 사용자 정보
    private Provider provider;
}
