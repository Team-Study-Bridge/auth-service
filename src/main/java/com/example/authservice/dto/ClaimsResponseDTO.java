package com.example.authservice.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class ClaimsResponseDTO {
    private Long id;
    private String nickname;
    private String profileImage;
}
