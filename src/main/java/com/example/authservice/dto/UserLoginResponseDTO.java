package com.example.authservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
@Schema(description = "로그인 응답 DTO")
public class UserLoginResponseDTO {

    @Schema(description = "로그인 성공 여부", example = "true")
    private boolean loggedIn;

    @Schema(description = "Access Token", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
    private String accessToken;

    @Schema(description = "로그인 응답 메시지", example = "환영합니다 영진짱 님")
    private String message;
}
