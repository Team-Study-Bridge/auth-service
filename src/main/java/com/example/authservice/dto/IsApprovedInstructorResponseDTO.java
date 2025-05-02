package com.example.authservice.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class IsApprovedInstructorResponseDTO {
    private final boolean success;
    private final String message;
}
