package com.example.authservice.service;

import com.example.authservice.config.security.CustomOAuth2User;
import com.example.authservice.dto.TokenRequestDTO;
import com.example.authservice.mapper.OAuth2UserMapper;
import com.example.authservice.model.User;
import com.example.authservice.type.Provider;
import com.example.authservice.type.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final OAuth2UserMapper oAuth2UserMapper;
    private final TokenProviderService tokenProviderService;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);

        Map<String, Object> response = (Map<String, Object>) oAuth2User.getAttributes().get("response");
        String providerId = (String) response.get("providerId");
        String email = (String) response.get("email");
        String name = (String) response.get("name");
        String phoneNumber = (String) response.get("phoneNumber");
        String provider = (String) response.get("provider");

        User existingUser = oAuth2UserMapper.findByUserIdAndProvider(providerId, Provider.valueOf(provider).name());

        if (existingUser == null) {
            User newUser = User.builder()
                    .providerId(providerId)
                    .nickname(name)
                    .phoneNumber(phoneNumber)
                    .email(email)
                    .provider(Provider.valueOf(provider))
                    .role(Role.STUDENT)  // OAuth로 가입한 사용자에게 기본 권한 부여 (필요 시 변경)
                    .build();

            oAuth2UserMapper.insertOAuthUser(newUser);
            existingUser = newUser;  // 이후 처리 위해 변수 업데이트
        }

        // ✅ TokenRequestDTO 생성
        TokenRequestDTO tokenRequestDTO = TokenRequestDTO.builder()
                .email(existingUser.getEmail())
                .nickname(existingUser.getNickname())
                .role(existingUser.getRole())
                .build();

        // ✅ 토큰 발급
        String accessToken = tokenProviderService.generateToken(tokenRequestDTO, Duration.ofHours(2));
        String refreshToken = tokenProviderService.generateToken(tokenRequestDTO, Duration.ofDays(2));

        return new CustomOAuth2User(existingUser, accessToken, refreshToken);
    }
}
