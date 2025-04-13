package com.example.authservice.dto;

import com.example.authservice.model.User;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class OAuthUserInfoResponseDTO {
    private boolean success;
    private String token;
    private User user;
    private boolean needsLinking;
    private String message;
}
