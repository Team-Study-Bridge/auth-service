package com.example.authservice.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ClaimsRequestDTO {
    private Long userId;
    private String nickname;
    private String profileImage;
}
