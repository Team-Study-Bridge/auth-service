package com.example.authservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "프로필 이미지 변경 응답 DTO")
public class ProfileImageUpdateResponseDTO {

    @Schema(description = "요청 성공 여부", example = "true")
    private boolean success;

    @Schema(description = "응답 메시지", example = "프로필 이미지가 성공적으로 업데이트되었습니다.")
    private String message;

    @Schema(description = "업데이트된 이미지 URL", example = "https://s3.amazonaws.com/mybucket/profile123.jpg")
    private String profileImage;
}
