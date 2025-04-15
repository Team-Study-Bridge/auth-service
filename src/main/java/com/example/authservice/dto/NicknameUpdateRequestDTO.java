package com.example.authservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "닉네임 변경 요청 DTO")
public class NicknameUpdateRequestDTO {

    @Schema(description = "새로 변경할 닉네임", example = "영진짱", requiredMode = Schema.RequiredMode.REQUIRED)
    private String nickname;
}
