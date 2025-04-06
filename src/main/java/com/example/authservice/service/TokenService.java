package com.example.authservice.service;

import com.example.authservice.dto.ClaimsResponseDTO;
import com.example.authservice.dto.RefreshTokenResponseDTO;
import com.example.authservice.dto.ValidTokenResponseDTO;
import com.example.authservice.util.CookieUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class TokenService {

    private final TokenProviderService tokenProviderService;


    public RefreshTokenResponseDTO refreshToken(String refreshToken,
                                                HttpServletResponse response,
                                                HttpServletRequest request) {
        int result = tokenProviderService.validateToken(refreshToken);

        if (result == 1) {
            // 기존 토큰에서 정보 추출 기존 닉네임, 프로필이미지url
            ClaimsResponseDTO claims = tokenProviderService.getAuthentication(refreshToken);

            ClaimsResponseDTO claimsResponseDTO = ClaimsResponseDTO.builder()
                    .nickname(claims.getNickname())
                    .profileImage(claims.getProfileImage())
                    .build();

            String newAccessToken = tokenProviderService.generateToken(claimsResponseDTO, Duration.ofHours(2));
            String newRefreshToken = tokenProviderService.generateToken(claimsResponseDTO, Duration.ofDays(2));

            CookieUtil.deleteCookie(request, response, "refreshToken");

            CookieUtil.addCookie(response, "refreshToken", newRefreshToken, 7 * 24 * 60 * 60);

            return RefreshTokenResponseDTO.builder()
                    .success(true)
                    .accessToken(newAccessToken)
                    .message("토큰이 재발급 되었습니다.")
                    .build();
        }

        return RefreshTokenResponseDTO.builder()
                .success(false)
                .message("토큰의 재발급을 실패하였습니다.")
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
