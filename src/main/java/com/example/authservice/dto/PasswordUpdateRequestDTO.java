package com.example.authservice.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PasswordUpdateRequestDTO {
    private String accessToken;
    private String currentPassword;
    private String newPassword;
}
