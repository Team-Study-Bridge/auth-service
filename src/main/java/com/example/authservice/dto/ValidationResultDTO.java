package com.example.authservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class ValidationResultDTO {

    private final boolean success;
    private final String passwordError;
    private final String nicknameError;

    public static ValidationResultDTO success() {
        return ValidationResultDTO.builder()
                .success(true)
                .passwordError(null)
                .nicknameError(null)
                .build();
    }

    public static ValidationResultDTO failure(String passwordError, String nicknameError) {
        return ValidationResultDTO.builder()
                .success(false)
                .passwordError(passwordError)
                .nicknameError(nicknameError)
                .build();
    }
}
