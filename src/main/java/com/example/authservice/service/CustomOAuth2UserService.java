package com.example.authservice.service;


import com.example.authservice.config.security.CustomOAuth2User;
import com.example.authservice.mapper.OAuth2UserMapper;
import com.example.authservice.model.User;
import com.example.authservice.type.Provider;
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
                    .providerId(providerId)  // 네이버에서 받은 고유 ID 저장
                    .nickname(name)  // 네이버에서 가져온 유저 이름을 닉네임에 저장
                    .phoneNumber(phoneNumber)
                    .email(email)
                    .provider(Provider.valueOf(provider))
                    .build();

            oAuth2UserMapper.insertOAuthUser(newUser);
            existingUser = newUser;  // 이후 처리 위해 변수 업데이트
        }
        String accessToken = tokenProviderService.generateToken(existingUser,  Duration.ofHours(2));
        String refreshToken = tokenProviderService.generateToken(existingUser, Duration.ofDays(2));

        return new CustomOAuth2User(existingUser, accessToken, refreshToken);
    }

}
