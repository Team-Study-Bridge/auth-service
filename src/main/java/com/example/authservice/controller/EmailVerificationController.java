package com.example.authservice.controller;

import com.example.authservice.dto.EmailRequestDTO;
import com.example.authservice.service.EmailVerificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/email")
@RequiredArgsConstructor
public class EmailVerificationController {

    private final EmailVerificationService emailVerificationService;

    @PostMapping("/send-code")
    public String sendVerificationCode(@RequestBody EmailRequestDTO requestDTO) {
        String email = requestDTO.getEmail();
        emailVerificationService.sendVerificationCode(email);
        return "Verification code sent successfully to " + email;
    }

    @PostMapping("/verify-code")
    public String verifyCode(@RequestBody EmailRequestDTO requestDTO) {
        boolean isVerified = emailVerificationService.verifyCode(requestDTO.getEmail(), requestDTO.getCode());
        return isVerified ? "Email verified successfully." : "Invalid code or already verified.";
    }
}
