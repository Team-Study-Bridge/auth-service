package com.example.authservice.config.security.oauth;

import com.example.authservice.dto.UserLoginResponseDTO;
import com.example.authservice.util.CookieUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

@Component
public class CustomOAuth2AuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    // ObjectMapper는 DI 받거나 new 로 생성할 수 있음.
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
        // CustomOAuth2User 안에는 accessToken (jwtToken)와 refreshToken 정보가 들어 있음
        CustomOAuth2User oauthUser = (CustomOAuth2User) authentication.getPrincipal();
        String refreshToken = oauthUser.getRefreshToken();
        String accessToken = oauthUser.getJwtToken();

        // refreshToken 쿠키에 저장 (유효기간 7일)
        CookieUtil.addCookie(response, "refreshToken", refreshToken, 7 * 24 * 60 * 60);

        // 프론트엔드에 반환할 응답 DTO 생성
        UserLoginResponseDTO userResponse = UserLoginResponseDTO.builder()
                .loggedIn(true)
                .accessToken(accessToken)
                .message("로그인 성공")
                .build();

        // 응답 콘텐츠 타입 JSON으로 설정 후 DTO를 JSON 문자열로 변환하여 응답 본문에 작성
        response.setContentType("application/json;charset=UTF-8");
        try {
            String jsonResponse = objectMapper.writeValueAsString(userResponse);
            response.getWriter().write(jsonResponse);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
