package com.example.authservice.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TokenResponseDTO {
    private String accessToken;
    private String refreshToken;
    private boolean success;
    private String message;
}