package com.example.authservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "닉네임 변경 응답 DTO")
public class NicknameUpdateResponseDTO {

    @Schema(description = "요청 성공 여부", example = "true")
    private boolean success;

    @Schema(description = "응답 메시지", example = "닉네임이 성공적으로 변경되었습니다.")
    private String message;

    @Schema(description = "변경된 닉네임", example = "영진짱")
    private String nickname;
}