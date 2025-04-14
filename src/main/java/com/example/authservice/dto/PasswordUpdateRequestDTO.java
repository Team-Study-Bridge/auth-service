package com.example.authservice.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PasswordUpdateRequestDTO {
    private String currentPassword;
    private String newPassword;
}
