package com.example.authservice.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class NicknameUpdateResponseDTO {
    private boolean success;
    private String message;
    private String nickname;
}
