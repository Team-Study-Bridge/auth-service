package com.example.authservice.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SendCodeResponseDTO {
    private boolean success;
    private String message;
}
