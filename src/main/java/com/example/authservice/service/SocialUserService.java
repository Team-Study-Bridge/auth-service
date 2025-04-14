package com.example.authservice.service;

import com.example.authservice.config.security.oauth.CustomOAuth2User;
import com.example.authservice.dto.ClaimsRequestDTO;
import com.example.authservice.dto.OAuthUserInfoResponseDTO;
import com.example.authservice.mapper.OAuth2UserMapper;
import com.example.authservice.model.User;
import com.example.authservice.type.Provider;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class SocialUserService {
    private final OAuth2UserMapper oAuth2UserMapper;
    private final TokenProviderService tokenProviderService;
    private final RedisTemplate<String, String> redisTemplate;

    public ResponseEntity<OAuthUserInfoResponseDTO> getUserInfo(CustomOAuth2User customOAuth2User) {
        if (customOAuth2User == null) {
            return ResponseEntity.status(401).body(
                    OAuthUserInfoResponseDTO.builder()
                            .success(false)
                            .message("Unauthorized")
                            .build()
            );
        }

        boolean needsLinking = customOAuth2User.isNeedsLinking();

        OAuthUserInfoResponseDTO responseDTO = OAuthUserInfoResponseDTO.builder()
                .success(!needsLinking)
                .token(customOAuth2User.getJwtToken())
                .user(customOAuth2User.getUser())
                .needsLinking(needsLinking)
                .message(needsLinking ? "기존 계정이 존재합니다. 연동하시겠습니까?" : "로그인 성공")
                .build();

        return ResponseEntity.ok(responseDTO);
    }

    public ResponseEntity<OAuthUserInfoResponseDTO> linkAccount(CustomOAuth2User customUser) {
        User user = customUser.getUser();

        if (user.getProvider() != Provider.LOCAL) {
            return ResponseEntity.badRequest().body(
                    OAuthUserInfoResponseDTO.builder()
                            .success(false)
                            .message("이미 연동된 계정입니다.")
                            .build()
            );
        }
        Provider provider = customUser.getProvider();

        oAuth2UserMapper.updateUserWithSocialInfo(
                user.getId(),
                user.getProviderId(),
                provider
        );

        ClaimsRequestDTO claims = ClaimsRequestDTO.builder()
                .nickname(user.getNickname())
                .profileImage(user.getProfileImage())
                .build();

        String accessToken = tokenProviderService.generateToken(claims, Duration.ofHours(2));
        String refreshToken = tokenProviderService.generateToken(claims, Duration.ofDays(7));

        redisTemplate.opsForValue().set("accessToken:" + user.getId(), accessToken, Duration.ofHours(2));
        redisTemplate.opsForValue().set("refreshToken:" + user.getId(), refreshToken, Duration.ofDays(7));

        return ResponseEntity.ok(
                OAuthUserInfoResponseDTO.builder()
                        .success(true)
                        .token(accessToken)
                        .user(user)
                        .needsLinking(false)
                        .message("연동 완료! 로그인 성공")
                        .build()
        );
    }
}
