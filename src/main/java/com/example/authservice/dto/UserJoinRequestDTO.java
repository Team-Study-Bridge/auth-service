package com.example.authservice.dto;

import com.example.authservice.model.User;
import com.example.authservice.type.Provider;
import com.example.authservice.type.Role;
import com.example.authservice.type.Status;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Getter
@Setter
@ToString
@Builder
@Schema(description = "회원가입 요청 DTO")
public class UserJoinRequestDTO {

    @Schema(description = "이메일", example = "test@example.com", requiredMode = Schema.RequiredMode.REQUIRED)
    private String email;

    @Schema(description = "비밀번호", example = "secure123!", requiredMode = Schema.RequiredMode.REQUIRED)
    private String password;

    @Schema(description = "닉네임", example = "영진짱", requiredMode = Schema.RequiredMode.REQUIRED)
    private String nickname;

    @Schema(description = "프로필 이미지 URL", example = "https://bucket.s3.amazonaws.com/profile.jpg")
    private String profileImage;

    @Schema(description = "역할 (예: STUDENT, INSTRUCTOR, ADMIN)", example = "STUDENT")
    private Role role;

    @Schema(description = "가입 제공자 (예: LOCAL, NAVER, GOOGLE)", example = "LOCAL")
    private Provider provider;

    @Schema(description = "계정 상태 (예: ACTIVE, INACTIVE)", example = "ACTIVE")
    private Status status;

    public User toUser(BCryptPasswordEncoder bCryptPasswordEncoder) {
        return User.builder()
                .email(email)
                .password(bCryptPasswordEncoder.encode(password))
                .nickname(nickname)
                .profileImage(profileImage)
                .role(role != null ? role : Role.STUDENT)
                .provider(provider != null ? provider : Provider.LOCAL)
                .status(status != null ? status : Status.ACTIVE)
                .build();
    }
}

