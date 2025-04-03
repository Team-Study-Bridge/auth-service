package com.example.authservice.controller;

import com.example.authservice.dto.UserJoinRequestDTO;
import com.example.authservice.dto.UserJoinResponseDTO;
import com.example.authservice.dto.UserLoginRequestDTO;
import com.example.authservice.dto.UserLoginResponseDTO;
import com.example.authservice.service.EmailVerificationService;
import com.example.authservice.service.UserService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auths")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final EmailVerificationService emailVerificationService;
    private final BCryptPasswordEncoder bCryptPasswordEncoder;

    @PostMapping("/join")
    public UserJoinResponseDTO join(@RequestBody UserJoinRequestDTO requestDTO) {

        // 이메일 인증 여부 확인
        if (!emailVerificationService.isVerified(requestDTO.getEmail())) {
            return UserJoinResponseDTO.builder()
                    .isSuccess(false)
                    .message("이메일 인증이 완료되지 않았습니다. 인증 후 다시 시도하세요.")
                    .build();
        }
        return userService.save(requestDTO.toUser(bCryptPasswordEncoder));
    }

    @PostMapping("/login")
    public UserLoginResponseDTO login(@RequestBody UserLoginRequestDTO userLoginRequestDTO, HttpServletResponse response) {
        return userService.login(userLoginRequestDTO.getEmail(), userLoginRequestDTO.getPassword(), response);
    }
}