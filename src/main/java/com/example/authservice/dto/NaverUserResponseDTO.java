package com.example.authservice.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class NaverUserResponseDTO {
    private String id;
    private String name;
    private String email;
}
