package com.example.authservice.dto;

import com.example.authservice.type.Provider;
import com.example.authservice.type.Role;
import com.example.authservice.type.Status;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UserInfoResponseDTO {
    private boolean success;
    private String message;
    private String email;
    private String nickname;
    private String phoneNumber;
    private Role role;
    private Provider provider;
    private Status status;
    private String profileImage;
}
