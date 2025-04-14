package com.example.authservice.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ApiErrorResponseDTO {
    private boolean success;
    private int code;
    private String message;
}