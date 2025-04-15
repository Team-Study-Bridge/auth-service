package com.example.authservice.dto;

import com.example.authservice.type.Provider;
import com.example.authservice.type.Role;
import com.example.authservice.type.Status;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "회원 정보 조회 응답 DTO")
public class UserInfoResponseDTO {

    @Schema(description = "요청 성공 여부", example = "true")
    private boolean success;

    @Schema(description = "응답 메시지", example = "사용자의 정보를 정상적으로 불러왔습니다.")
    private String message;

    @Schema(description = "회원 이메일", example = "user@example.com")
    private String email;

    @Schema(description = "회원 닉네임", example = "영진짱")
    private String nickname;

    @Schema(description = "회원 전화번호", example = "010-1234-5678")
    private String phoneNumber;

    @Schema(description = "회원 권한", example = "STUDENT")
    private Role role;

    @Schema(description = "소셜 로그인 제공자", example = "LOCAL")
    private Provider provider;

    @Schema(description = "회원 상태", example = "ACTIVE")
    private Status status;

    @Schema(description = "프로필 이미지 URL", example = "https://s3.amazonaws.com/mybucket/profile123.jpg")
    private String profileImage;
}
