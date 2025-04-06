package com.example.authservice.util;

import com.example.authservice.dto.ValidationResultDTO;
import lombok.Getter;

import java.util.regex.Pattern;

@Getter
public class Validator {

    private static final String PASSWORD_PATTERN = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[@$!%*#?&])[A-Za-z\\d@$!%*#?&]{8,}$";
    private static final String NICKNAME_PATTERN = "^[a-zA-Z0-9가-힣]{2,20}$";

    public static ValidationResultDTO validateUserInput(String password, String nickname) {

        ValidationResultDTO validationResult = ValidationResultDTO.builder()
                .success(true)
                .build();

        if (!Pattern.matches(PASSWORD_PATTERN, password)) {
            validationResult.setSuccess(false);
            validationResult.setPasswordError("비밀번호는 최소 8자 이상, 영문, 숫자, 특수문자를 포함해야 합니다.");
        }

        if (!Pattern.matches(NICKNAME_PATTERN, nickname)) {
            validationResult.setSuccess(false);
            validationResult.setNicknameError("닉네임은 2~20자이며, 공백이나 특수문자를 포함할 수 없습니다.");
        }

        return validationResult;
    }
}