package com.example.authservice.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
public class OAuthCodeRequestDTO {
    private String code;
    private String state;
}