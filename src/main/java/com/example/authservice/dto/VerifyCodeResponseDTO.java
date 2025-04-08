package com.example.authservice.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class VerifyCodeResponseDTO {
    private Boolean success;
    private String message;
}
