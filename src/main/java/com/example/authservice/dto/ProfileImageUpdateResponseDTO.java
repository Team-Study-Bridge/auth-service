package com.example.authservice.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ProfileImageUpdateResponseDTO {
    private boolean success;
    private String message;
    private String profileImage;
}
