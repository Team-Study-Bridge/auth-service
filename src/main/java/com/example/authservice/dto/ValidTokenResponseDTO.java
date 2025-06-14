package com.example.authservice.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ValidTokenResponseDTO {
    private boolean isValid;
    private String message;
}
