package com.example.authservice.config;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;

class SecurityConfigTest {

    @Test
    void bCryptPasswordEncoder() {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String rawPassword = "password123";
        String encodedPassword = encoder.encode(rawPassword);

        // BCryptPasswordEncoder.matches() 메서드를 사용해 비밀번호가 일치하는지 검증
        assertTrue(encoder.matches(rawPassword, encodedPassword));
    }
}