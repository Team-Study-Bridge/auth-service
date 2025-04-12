package com.example.authservice.service;

import com.example.authservice.dto.*;
import com.example.authservice.util.CookieUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class TokenService {

    private final TokenProviderService tokenProviderService;
    private final RedisTemplate<String, Object> redisTemplate;


    public ResponseEntity<RefreshTokenResponseDTO> refreshToken(String accessToken,
                                                                HttpServletResponse response,
                                                                HttpServletRequest request) {
        // 1. accessToken 유효성 검사
        int result = tokenProviderService.validateToken(accessToken);
        if (result != 1) {
            return ResponseEntity.status(HttpServletResponse.SC_UNAUTHORIZED).body(
                    RefreshTokenResponseDTO.builder()
                            .success(false)
                            .message("유효하지 않은 액세스 토큰입니다.")
                            .build()
            );
        }

        // 2. accessToken에서 userId 추출
        ClaimsResponseDTO claims;
        try {
            claims = tokenProviderService.getAuthentication(accessToken);
        } catch (Exception e) {
            return ResponseEntity.status(HttpServletResponse.SC_UNAUTHORIZED).body(
                    RefreshTokenResponseDTO.builder()
                            .success(false)
                            .message("토큰 파싱 중 오류가 발생했습니다.")
                            .build()
            );
        }

        Long userId = claims.getId();
        if (userId == null) {
            return ResponseEntity.status(HttpServletResponse.SC_UNAUTHORIZED).body(
                    RefreshTokenResponseDTO.builder()
                            .success(false)
                            .message("토큰에 유효한 사용자 정보가 없습니다.")
                            .build()
            );
        }

        // 3. 쿠키에서 refreshToken 추출
        String refreshTokenFromCookie = CookieUtil.getCookieValue(request, "refreshToken");
        if (refreshTokenFromCookie == null) {
            return ResponseEntity.status(HttpServletResponse.SC_UNAUTHORIZED).body(
                    RefreshTokenResponseDTO.builder()
                            .success(false)
                            .message("리프레시 토큰이 존재하지 않습니다.")
                            .build()
            );
        }

        // 4. Redis에서 저장된 refreshToken 확인
        String savedRefreshToken = (String) redisTemplate.opsForValue().get("refresh:" + userId);
        if (savedRefreshToken == null || !savedRefreshToken.equals(refreshTokenFromCookie)) {
            return ResponseEntity.status(HttpServletResponse.SC_UNAUTHORIZED).body(
                    RefreshTokenResponseDTO.builder()
                            .success(false)
                            .message("유효하지 않은 리프레시 토큰입니다.")
                            .build()
            );
        }

        // 5. 새 토큰 생성
        ClaimsRequestDTO claimsRequestDTO = ClaimsRequestDTO.builder()
                .nickname(claims.getNickname())
                .profileImage(claims.getProfileImage())
                .userId(userId)
                .build();

        String newAccessToken = tokenProviderService.generateToken(claimsRequestDTO, Duration.ofMinutes(2));
        String newRefreshToken = tokenProviderService.generateToken(claimsRequestDTO, Duration.ofDays(7));

        // 6. Redis 갱신 (accessToken, refreshToken)
        redisTemplate.opsForValue().set("accessToken:" + userId, newAccessToken, Duration.ofMinutes(2));
        redisTemplate.opsForValue().set("refreshToken:" + userId, newRefreshToken, Duration.ofDays(7));

        // 7. 쿠키 갱신
        CookieUtil.deleteCookie(request, response, "refreshToken");
        CookieUtil.addCookie(response, "refreshToken", newRefreshToken, 7 * 24 * 60 * 60);

        // 8. 성공 응답
        return ResponseEntity.ok(
                RefreshTokenResponseDTO.builder()
                        .success(true)
                        .accessToken(newAccessToken)
                        .message("토큰이 성공적으로 재발급 되었습니다.")
                        .build()
        );
    }


    public ValidTokenResponseDTO validateToken(String token) {
        int result = tokenProviderService.validateToken(token);

        return ValidTokenResponseDTO.builder()
                .isValid(result == 1)
                .message(result == 1 ? "유효한 토큰입니다." : result == 2 ? "만료된 토큰입니다." : "잘못된 토큰입니다.")
                .build();
    }
}
