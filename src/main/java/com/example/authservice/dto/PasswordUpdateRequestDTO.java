package com.example.authservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "비밀번호 변경 요청 DTO")
public class PasswordUpdateRequestDTO {

    @Schema(description = "현재 비밀번호", example = "oldPass123!", requiredMode = Schema.RequiredMode.REQUIRED)
    private String currentPassword;

    @Schema(description = "새 비밀번호", example = "newPass456!", requiredMode = Schema.RequiredMode.REQUIRED)
    private String newPassword;
}
