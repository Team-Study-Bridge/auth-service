package com.example.authservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "회원가입 응답 DTO")
public class UserJoinResponseDTO {

    @Schema(description = "회원가입 성공 여부", example = "true")
    private boolean success;

    @Schema(description = "Access Token", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
    private String accessToken;

    @Schema(description = "응답 코드 (사용 안 하면 생략 가능)", example = "200")
    private int code;

    @Schema(description = "유효성 검사 오류 필드", nullable = true)
    private ValidationResultDTO errors;

    @Schema(description = "응답 메시지", example = "회원가입 및 로그인 완료")
    private String message;
}
