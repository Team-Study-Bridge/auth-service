package com.example.authservice.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UserJoinResponseDTO {
    private boolean success;
    private String accessToken;
    private ValidationResultDTO errors;
    private String message;
}
