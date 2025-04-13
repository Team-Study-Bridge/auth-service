package com.example.authservice.service;

import com.example.authservice.config.security.oauth.CustomOAuth2User;
import com.example.authservice.dto.ClaimsRequestDTO;
import com.example.authservice.dto.NaverUserResponseDTO;
import com.example.authservice.mapper.OAuth2UserMapper;
import com.example.authservice.mapper.UserMapper;
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
    private final UserMapper userMapper;


    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);
        String provider = userRequest.getClientRegistration().getRegistrationId();

        if ("naver".equalsIgnoreCase(provider)) {
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

            User existingUser = oAuth2UserMapper.findByUserIdAndProvider(providerId, Provider.valueOf(provider.toUpperCase()));
            boolean needsLinking = false;

            if (existingUser == null) {
                User byEmail = userMapper.findByEmail(email);
                if (byEmail != null && byEmail.getProvider() == null) {
                    existingUser = byEmail;
                    needsLinking = true;
                } else if (byEmail == null) {
                    User newUser = User.builder()
                            .providerId(providerId)
                            .nickname(name)
                            .email(email)
                            .provider(Provider.valueOf(provider.toUpperCase()))
                            .role(Role.STUDENT)
                            .build();

                    oAuth2UserMapper.insertOAuthUser(newUser);
                    existingUser = newUser;
                }
            }

            ClaimsRequestDTO claimsRequestDTO = ClaimsRequestDTO.builder()
                    .nickname(existingUser.getNickname())
                    .profileImage(existingUser.getProfileImage())
                    .build();

            String accessToken = tokenProviderService.generateToken(claimsRequestDTO, Duration.ofHours(2));
            String refreshToken = tokenProviderService.generateToken(claimsRequestDTO, Duration.ofDays(7));

            redisTemplate.opsForValue().set("accessToken:" + existingUser.getId(), accessToken,  Duration.ofHours(2));
            redisTemplate.opsForValue().set("refreshToken:" + existingUser.getId(), refreshToken, Duration.ofDays(7));

            CustomOAuth2User customOAuth2User = new CustomOAuth2User(existingUser, accessToken, refreshToken);
            customOAuth2User.setNeedsLinking(needsLinking);

            return customOAuth2User;
        } else {
            throw new OAuth2AuthenticationException("지원하지 않는 소셜 로그인 제공자입니다.");
        }
    }
}
