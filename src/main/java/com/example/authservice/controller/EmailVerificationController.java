package com.example.authservice.controller;

import com.example.authservice.dto.EmailRequestDTO;
import com.example.authservice.dto.SendCodeResponseDTO;
import com.example.authservice.dto.VerifyCodeResponseDTO;
import com.example.authservice.service.EmailVerificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/email")
@RequiredArgsConstructor
public class EmailVerificationController {

    private final EmailVerificationService emailVerificationService;

    @PostMapping("/send-code")
    public SendCodeResponseDTO sendVerificationCode(@RequestBody EmailRequestDTO emailRequestDTO) {
        return emailVerificationService.sendVerificationCode(emailRequestDTO.getEmail());
    }

    @PostMapping("/verify-code")
    public VerifyCodeResponseDTO verifyCode(@RequestBody EmailRequestDTO emailRequestDTO) {
        return emailVerificationService.verifyCode(emailRequestDTO.getEmail(), emailRequestDTO.getCode());
    }
}
