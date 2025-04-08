package com.example.authservice.service;

import com.example.authservice.config.security.CustomOAuth2User;
import com.example.authservice.dto.ClaimsRequestDTO;
import com.example.authservice.dto.NaverUserLoginRequestDTO;
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
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final OAuth2UserMapper oAuth2UserMapper;
    private final TokenProviderService tokenProviderService;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);

//        NaverUserLoginRequestDTO.builder()
//                .providerId(userRequest.getClientRegistration().getRegistrationId())
//                .name(userRequest.getClientRegistration().getClientName())
//                .phoneNumber()
//                .email()
//                .name(oAuth2User.getName())
//                .build();

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

        ClaimsRequestDTO claimsRequestDTO = ClaimsRequestDTO.builder()
                .nickname(existingUser.getNickname())
                .profileImage(existingUser.getProfileImage())
                .build();

        // ✅ 토큰 발급
        String accessToken = tokenProviderService.generateToken(tokenRequestDTO, Duration.ofHours(2));
        String refreshToken = tokenProviderService.generateToken(tokenRequestDTO, Duration.ofDays(2));

        return new CustomOAuth2User(existingUser, accessToken, refreshToken);
    }
}
