package com.example.authservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Getter
@Schema(description = "로그인 요청 DTO")
public class UserLoginRequestDTO {

    @Schema(description = "이메일", example = "test@example.com", requiredMode = Schema.RequiredMode.REQUIRED)
    private String email;

    @Schema(description = "비밀번호", example = "secure123!", requiredMode = Schema.RequiredMode.REQUIRED)
    private String password;
}
