package com.example.authservice.service;

import com.example.authservice.config.jwt.JwtProperties;
import com.example.authservice.dto.ClaimsRequestDTO;
import com.example.authservice.dto.ClaimsResponseDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;

class TokenProviderServiceTest {

    private TokenProviderService tokenProviderService;
    private JwtProperties jwtProperties;

    @BeforeEach
    void setUp() {
        jwtProperties = new JwtProperties();

        // ✅ 바이트 배열로 강력한 시크릿 키 생성
        byte[] keyBytes = new byte[64]; // 512비트
        for (int i = 0; i < 64; i++) {
            keyBytes[i] = (byte) (i + 1);
        }

        String base64Key = Base64.getEncoder().encodeToString(keyBytes);
        jwtProperties.setSecretKey(base64Key);
        jwtProperties.setIssuer("testIssuer");

        tokenProviderService = new TokenProviderService(jwtProperties);

        // 디버깅용 출력
        System.out.println("Base64 디코딩된 키 길이: " + Base64.getDecoder().decode(jwtProperties.getSecretKey()).length); // 👉 64 출력되어야 성공
    }

    @Test
    void generateToken_정상생성() {
        ClaimsRequestDTO claims = ClaimsRequestDTO.builder()
                .userId(1L)
                .nickname("Tester")
                .profileImage("image.png")
                .build();

        String token = tokenProviderService.generateToken(claims, Duration.ofMinutes(5));
        assertThat(token).isNotNull();
        assertThat(token).contains(".");
    }

    @Test
    void validateToken_정상토큰() {
        String token = createValidToken();
        int result = tokenProviderService.validateToken(token);
        assertThat(result).isEqualTo(1);
    }

    @Test
    void validateToken_만료토큰() throws InterruptedException {
        ClaimsRequestDTO claims = ClaimsRequestDTO.builder()
                .userId(1L)
                .nickname("OldUser")
                .profileImage(null)
                .build();

        // 유효기간 0.5초짜리 토큰
        String token = tokenProviderService.generateToken(claims, Duration.ofMillis(500));

        // 만료될 때까지 대기
        Thread.sleep(600);

        int result = tokenProviderService.validateToken(token);
        assertThat(result).isEqualTo(2);
    }

    @Test
    void validateToken_이상한토큰() {
        String token = "this.is.not.valid";
        int result = tokenProviderService.validateToken(token);
        assertThat(result).isEqualTo(3);
    }

    @Test
    void getAuthentication_정상() {
        ClaimsRequestDTO claims = ClaimsRequestDTO.builder()
                .userId(42L)
                .nickname("Nickname")
                .profileImage("img.jpg")
                .build();

        String token = tokenProviderService.generateToken(claims, Duration.ofMinutes(10));
        ClaimsResponseDTO extracted = tokenProviderService.getAuthentication(token);

        assertThat(extracted.getId()).isEqualTo(42L);
        assertThat(extracted.getNickname()).isEqualTo("Nickname");
        assertThat(extracted.getProfileImage()).isEqualTo("img.jpg");
    }

    // 헬퍼 메서드
    private String createValidToken() {
        ClaimsRequestDTO claims = ClaimsRequestDTO.builder()
                .userId(100L)
                .nickname("ValidUser")
                .profileImage(null)
                .build();

        return tokenProviderService.generateToken(claims, Duration.ofMinutes(5));
    }
}
