package com.example.authservice.service;

import com.example.authservice.config.social.NaverOAuthProperties;
import com.example.authservice.dto.*;
import com.example.authservice.mapper.OAuth2UserMapper;
import com.example.authservice.mapper.UserMapper;
import com.example.authservice.model.User;
import com.example.authservice.type.Provider;
import com.example.authservice.type.Role;
import com.example.authservice.util.CookieUtil;
import com.example.authservice.util.TokenUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.time.Duration;

@Service
@RequiredArgsConstructor
public class OAuthService {

    private final TokenProviderService tokenProviderService;
    private final RedisTemplate<String, String> redisTemplate;
    private final OAuth2UserMapper oAuth2UserMapper;
    private final UserMapper userMapper;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;
    private final NaverOAuthProperties oauthProperties;
    private final TokenUtil tokenUtil;

    public ResponseEntity<OAuthLoginResponseDTO> loginWithNaverCode(String code, String state, HttpServletResponse response) {
        String tokenUrl = "https://nid.naver.com/oauth2.0/token" +
                "?grant_type=authorization_code" +
                "&client_id=" + oauthProperties.getClientId() +
                "&client_secret=" + oauthProperties.getClientSecret() +
                "&code=" + code +
                "&state=" + state;

        ResponseEntity<String> tokenResponse = restTemplate.getForEntity(tokenUrl, String.class);
        if (!tokenResponse.getStatusCode().is2xxSuccessful()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    OAuthLoginResponseDTO.builder()
                            .success(false)
                            .message("네이버 토큰 요청 실패")
                            .build()
            );
        }

        NaverTokenResponse token = parse(tokenResponse.getBody(), NaverTokenResponse.class);

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + token.getAccessToken());
        HttpEntity<?> entity = new HttpEntity<>(headers);

        ResponseEntity<NaverUserWrapper> userInfoResponse = restTemplate.exchange(
                "https://openapi.naver.com/v1/nid/me",
                HttpMethod.GET,
                entity,
                NaverUserWrapper.class
        );

        NaverUserResponseDTO userInfo = userInfoResponse.getBody().getResponse();
        String email = userInfo.getEmail();
        String name = userInfo.getName();
        String providerId = userInfo.getId();

        User user = oAuth2UserMapper.findByUserIdAndProvider(providerId, Provider.NAVER);
        boolean needsLinking = false;

        if (user == null) {
            User byEmail = userMapper.findByEmail(email);

            if (byEmail != null) {
                // 기존 회원이 있고, NAVER로 가입한 게 아니면 연동
                if (byEmail.getProvider() != Provider.NAVER) {
                    user = byEmail;
                    needsLinking = true;
                } else {
                    // NAVER로 가입된 기존 사용자라면 그대로 로그인
                    user = byEmail;
                }
            } else {
                // 기존 이메일 없음 → 새 유저로 회원가입
                user = User.builder()
                        .providerId(providerId)
                        .nickname(name)
                        .email(email)
                        .provider(Provider.NAVER)
                        .role(Role.STUDENT)
                        .build();
                oAuth2UserMapper.insertOAuthUser(user);
            }
        }

        ClaimsRequestDTO claims = ClaimsRequestDTO.builder()
                .userId(user.getId())
                .nickname(user.getNickname())
                .profileImage(user.getProfileImage())
                .build();

        String accessToken = tokenProviderService.generateToken(claims, Duration.ofHours(2));
        String refreshToken = tokenProviderService.generateToken(claims, Duration.ofDays(7));

        CookieUtil.addCookie(response, "refreshToken", refreshToken, 7 * 24 * 60 * 60);

        redisTemplate.opsForValue().set("accessToken:" + user.getId(), accessToken, Duration.ofHours(2));
        redisTemplate.opsForValue().set("refreshToken:" + user.getId(), refreshToken, Duration.ofDays(7));

        return ResponseEntity.ok(
                OAuthLoginResponseDTO.builder()
                        .success(true)
                        .accessToken(accessToken)
                        .refreshToken(refreshToken)
                        .needsLinking(needsLinking)
                        .user(UserInfoDTO.builder()
                                .id(user.getId())
                                .email(user.getEmail())
                                .nickname(user.getNickname())
                                .profileImage(user.getProfileImage())
                                .provider(Provider.NAVER)
                                .build())
                        .message("소셜 로그인 성공")
                        .build()
        );
    }

    public ResponseEntity<OAuthUserInfoResponseDTO> linkAccount(String accessToken, Provider provider, HttpServletResponse response) {
        String cleanBearerToken = tokenUtil.cleanBearerToken(accessToken);

        ClaimsResponseDTO claims = tokenProviderService.getAuthentication(cleanBearerToken);
        Long userId = claims.getId();

        User user = userMapper.findById(userId);

        if (user.getProvider() != Provider.LOCAL) {
            return ResponseEntity.badRequest().body(
                    OAuthUserInfoResponseDTO.builder()
                            .success(false)
                            .message("이미 연동된 계정입니다.")
                            .build()
            );
        }

        oAuth2UserMapper.updateUserWithSocialInfo(
                user.getId(),
                user.getProviderId(),
                provider
        );

        ClaimsRequestDTO newClaims = ClaimsRequestDTO.builder()
                .userId(user.getId())
                .nickname(user.getNickname())
                .profileImage(user.getProfileImage())
                .build();

        String newAccessToken = tokenProviderService.generateToken(newClaims, Duration.ofHours(2));
        String refreshToken = tokenProviderService.generateToken(newClaims, Duration.ofDays(7));

        CookieUtil.addCookie(response, "refreshToken", refreshToken, 7 * 24 * 60 * 60);

        redisTemplate.opsForValue().set("accessToken:" + user.getId(), newAccessToken, Duration.ofHours(2));
        redisTemplate.opsForValue().set("refreshToken:" + user.getId(), refreshToken, Duration.ofDays(7));

        return ResponseEntity.ok(
                OAuthUserInfoResponseDTO.builder()
                        .success(true)
                        .token(newAccessToken)
                        .user(user)
                        .needsLinking(false)
                        .message("연동 완료! 로그인 성공")
                        .build()
        );
    }


    private <T> T parse(String body, Class<T> valueType) {
        try {
            return objectMapper.readValue(body, valueType);
        } catch (Exception e) {
            throw new RuntimeException("JSON 파싱 실패", e);
        }
    }
}
