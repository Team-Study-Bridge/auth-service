package com.example.authservice.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TokenRequestDTO {
    private String nickname;
    private String profileImage;
}