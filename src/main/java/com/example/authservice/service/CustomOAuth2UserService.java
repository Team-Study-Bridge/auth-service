package com.example.authservice.service;

import com.example.authservice.config.security.oauth.CustomOAuth2User;
import com.example.authservice.dto.ClaimsRequestDTO;
import com.example.authservice.dto.NaverUserResponseDTO;
import com.example.authservice.mapper.OAuth2UserMapper;
import com.example.authservice.model.User;
import com.example.authservice.type.Provider;
import com.example.authservice.type.Role;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final OAuth2UserMapper oAuth2UserMapper;
    private final TokenProviderService tokenProviderService;
    private final ObjectMapper objectMapper;
    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        // 기본 사용자 정보 로딩
        OAuth2User oAuth2User = super.loadUser(userRequest);
        String provider = userRequest.getClientRegistration().getRegistrationId(); // ex: "naver"

        if ("naver".equalsIgnoreCase(provider)) {
            // DTO로 변환 (네이버 응답은 "response"라는 키 아래에 실제 유저 정보가 들어있음)
            NaverUserResponseDTO naverUser;
            try {
                Object responseData = oAuth2User.getAttributes().get("response");
                naverUser = objectMapper.convertValue(responseData, NaverUserResponseDTO.class);
            } catch (IllegalArgumentException e) {
                throw new OAuth2AuthenticationException("네이버 OAuth2 응답을 DTO로 변환하는 과정에서 오류 발생");
            }

            String providerId = naverUser.getId();
            String name = naverUser.getName();
            String email = naverUser.getEmail();
            String phoneNumber = naverUser.getMobile();

            // DB에서 기존 사용자가 있는지 확인 (provider는 Enum 대문자로 비교)
            User existingUser = oAuth2UserMapper.findByUserIdAndProvider(providerId, Provider.valueOf(provider.toUpperCase()));

            if (existingUser == null) {
                User newUser = User.builder()
                        .providerId(providerId)
                        .nickname(name)
                        .phoneNumber(phoneNumber)
                        .email(email)
                        .provider(Provider.valueOf(provider.toUpperCase()))
                        .role(Role.STUDENT)
                        .build();

                oAuth2UserMapper.insertOAuthUser(newUser);
                existingUser = newUser;
            }

            // ClaimsRequestDTO 생성 (토큰 발급 시 필요한 사용자 정보)
            ClaimsRequestDTO claimsRequestDTO = ClaimsRequestDTO.builder()
                    .nickname(existingUser.getNickname())
                    .profileImage(existingUser.getProfileImage())
                    .build();

            // JWT 토큰, 리프레시 토큰 발급
            String accessToken = tokenProviderService.generateToken(claimsRequestDTO, Duration.ofHours(2));
            String refreshToken = tokenProviderService.generateToken(claimsRequestDTO, Duration.ofDays(7));

            // Redis에 토큰 저장
            redisTemplate.opsForValue().set("accessToken:" + accessToken, String.valueOf(existingUser.getId()), Duration.ofHours(2));
            redisTemplate.opsForValue().set("refreshToken:" + refreshToken, String.valueOf(existingUser.getId()), Duration.ofDays(7));

            // 쿠키 저장 로직은 현재 Service 계층에서는 Response 객체를 다룰 수 없으므로,
            // 별도의 OAuth2 인증 성공 후처리 핸들러(AuthenticationSuccessHandler) 또는 Filter에서 처리하도록 구성한다.
            // 예: ((CustomOAuth2User) oauth2User).getRefreshToken() 으로 값 가져와서, 성공 핸들러에서 CookieUtil.addCookie(response, "refreshToken", refreshToken, maxAge) 호출

            return new CustomOAuth2User(existingUser, accessToken, refreshToken);
        } else {
            throw new OAuth2AuthenticationException("지원하지 않는 소셜 로그인 제공자입니다.");
        }
    }
}
