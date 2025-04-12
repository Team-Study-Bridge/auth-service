package com.example.authservice.util;

import com.example.authservice.dto.ValidationResultDTO;
import lombok.Getter;

import java.util.regex.Pattern;

@Getter
public class Validator {

    private static final String PASSWORD_PATTERN = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[@$!%*#?&])[A-Za-z\\d@$!%*#?&]{8,}$";
    private static final String NICKNAME_PATTERN = "^[a-zA-Z0-9가-힣]{2,20}$";

    public static boolean validateNickname(String nickname) {
        return Pattern.matches(NICKNAME_PATTERN, nickname);
    }

    public static boolean validatePassword(String newPassword) {
        return Pattern.matches(PASSWORD_PATTERN, newPassword);
    }

    public static ValidationResultDTO validateUserInput(String password, String nickname) {

        boolean success = true;
        String passwordError = null;
        String nicknameError = null;

        if (!Pattern.matches(PASSWORD_PATTERN, password)) {
            success = false;
            passwordError = "비밀번호는 최소 8자 이상, 영문, 숫자, 특수문자를 포함해야 합니다.";
        }

        if (!Pattern.matches(NICKNAME_PATTERN, nickname)) {
            success = false;
            nicknameError = "닉네임은 2~20자이며, 공백이나 특수문자를 포함할 수 없습니다.";
        }

        if (success) {
            return ValidationResultDTO.success();
        } else {
            return ValidationResultDTO.failure(passwordError, nicknameError);
        }
    }

}