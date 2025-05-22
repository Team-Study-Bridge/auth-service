package com.example.authservice.service;

import com.example.authservice.dto.SendCodeResponseDTO;
import com.example.authservice.dto.VerifyCodeResponseDTO;
import org.springframework.http.ResponseEntity;

public interface EmailVerificationService {
    String generateCode();
    ResponseEntity<SendCodeResponseDTO> sendVerificationCode(String email);
    void sendEmail(String toEmail, String code);
    ResponseEntity<VerifyCodeResponseDTO> verifyCode(String email, String code);
    boolean isVerified(String email);
    void deleteEmailVerification(String email);
}
