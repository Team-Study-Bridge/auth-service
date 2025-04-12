package com.example.authservice.dto;

import lombok.*;

@Getter
@Builder
public class NaverUserResponseDTO {
    private String id;        // 네이버 유저의 고유 ID
    private String name;      // 유저 이름
    private String email;     // 이메일
    private String mobile;    // 휴대폰 번호 (네이버에서는 mobile 키로 전달)
    private int code;
}
