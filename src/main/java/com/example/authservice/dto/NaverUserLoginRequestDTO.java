package com.example.authservice.dto;

import com.example.authservice.type.Provider;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class NaverUserLoginRequestDTO {
    private String providerId;
    private String email;
    private String name;
    private String phoneNumber;
    private Provider provider;
}
