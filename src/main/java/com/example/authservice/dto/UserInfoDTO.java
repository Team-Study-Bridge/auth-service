package com.example.authservice.dto;

import com.example.authservice.type.Provider;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UserInfoDTO {
    private Long id;
    private String email;
    private String nickname;
    private String profileImage;
    private Provider provider; // "NAVER"
}

