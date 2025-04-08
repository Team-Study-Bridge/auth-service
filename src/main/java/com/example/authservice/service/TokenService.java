package com.example.authservice.service;

import com.example.authservice.dto.*;
import com.example.authservice.util.CookieUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class TokenService {

    private final TokenProviderService tokenProviderService;
    private final RedisTemplate<String, Object> redisTemplate;



    public RefreshTokenResponseDTO refreshToken(String refreshToken,
                                                HttpServletResponse response,
                                                HttpServletRequest request) {
        int result = tokenProviderService.validateToken(refreshToken);

        if (result != 1) {
            return RefreshTokenResponseDTO.builder()
                    .success(false)
                    .message("유효하지 않은 Token 입니다.")
                    .build();
        }


        String userIdStr = String.valueOf(redisTemplate.opsForValue().get("refreshToken:" + refreshToken));
        Long userId = userIdStr != null ? Long.parseLong(userIdStr) : null;
        if (userId == null) {
            return RefreshTokenResponseDTO.builder()
                    .success(false)
                    .message("잘못된 접근방식 입니다.")
                    .build();
        }

        ClaimsResponseDTO claims;
        try {
            claims = tokenProviderService.getAuthentication(refreshToken);
        } catch (Exception e) {
            return RefreshTokenResponseDTO.builder()
                    .success(false)
                    .message("유효하지 않은 토큰입니다. 다시 로그인하세요.")
                    .build();
        }

        ClaimsRequestDTO claimsRequestDTO = ClaimsRequestDTO.builder()
                .nickname(claims.getNickname())
                .profileImage(claims.getProfileImage())
                .build();

        String newAccessToken = tokenProviderService.generateToken(claimsRequestDTO, Duration.ofHours(2));
        String newRefreshToken = tokenProviderService.generateToken(claimsRequestDTO, Duration.ofDays(7));

        redisTemplate.delete("refreshToken:" + refreshToken);
        redisTemplate.opsForValue().set("accessToken:" + userId, newAccessToken, Duration.ofHours(2));
        redisTemplate.opsForValue().set("refreshToken:" + newRefreshToken, userId, Duration.ofDays(7));

        // 7. 기존 쿠키 삭제 및 새로운 쿠키 추가
        CookieUtil.deleteCookie(request, response, "refreshToken");
        CookieUtil.addCookie(response, "refreshToken", newRefreshToken, 7 * 24 * 60 * 60);

        // 8. 새로운 AccessToken 반환
        return RefreshTokenResponseDTO.builder()
                .success(true)
                .accessToken(newAccessToken)
                .message("토큰이 성공적으로 재발급 되었습니다.")
                .build();
    }


    public ValidTokenResponseDTO validateToken(String token) {
        int result = tokenProviderService.validateToken(token);

        return ValidTokenResponseDTO.builder()
                .isValid(result == 1)
                .message(result == 1 ? "유효한 토큰입니다." : result == 2 ? "만료된 토큰입니다." : "잘못된 토큰입니다.")
                .build();
    }
}
