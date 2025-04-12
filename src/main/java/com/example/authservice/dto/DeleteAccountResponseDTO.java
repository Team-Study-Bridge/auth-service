package com.example.authservice.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class DeleteAccountResponseDTO {
    private boolean success;
    private String message;
}
