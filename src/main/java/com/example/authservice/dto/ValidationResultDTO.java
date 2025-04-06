package com.example.authservice.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class ValidationResultDTO {
    private boolean success;
    private String passwordError;
    private String nicknameError;
}
