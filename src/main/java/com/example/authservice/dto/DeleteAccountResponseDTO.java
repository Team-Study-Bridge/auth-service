package com.example.authservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "회원 탈퇴 응답 DTO")
public class DeleteAccountResponseDTO {

    @Schema(description = "요청 성공 여부", example = "true")
    private boolean success;

    @Schema(description = "응답 메시지", example = "계정이 정상적으로 삭제되었습니다.")
    private String message;
}

