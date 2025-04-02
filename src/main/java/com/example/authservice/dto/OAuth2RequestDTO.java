package com.example.authservice.dto;

import com.example.authservice.type.Provider;
import com.example.authservice.type.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@Builder
public class OAuth2RequestDTO {
    private String email;           // 네이버 로그인으로 받아온 이메일
    private String name;            // 네이버 로그인으로 받아온 이름
    private String phoneNumber;     // 네이버 로그인으로 받아온 전화번호
    private String providerId;      // 네이버에서 제공하는 고유 ID
    private Provider provider;      // Provider 정보
    private Role role;              // 기본 설정으로는 STUDENT (추후 변경 가능)
}