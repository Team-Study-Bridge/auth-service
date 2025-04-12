package com.example.authservice.config.security.oauth;

import com.example.authservice.dto.UserLoginResponseDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class CustomOAuth2AuthenticationFailureHandler implements AuthenticationFailureHandler {

    // ObjectMapper를 사용해 DTO를 JSON 문자열로 변환
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void onAuthenticationFailure(HttpServletRequest request,
                                        HttpServletResponse response,
                                        AuthenticationException exception) throws IOException, ServletException {
        // 401 Unauthorized 상태 코드 설정
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");

        System.out.println("username: " + exception.getMessage());
        System.out.println("email: " + request.getRemoteAddr());
        System.out.println("authorities: " + exception.getMessage());
        System.out.println("refreshToken: " + request.getHeader("refreshToken"));
        System.out.println("accessToken: " + request.getHeader("accessToken"));
        System.out.println("authorities: " + exception.getMessage());
        System.out.println("실패");
        // 실패 시 반환할 DTO 생성 (loggedIn은 false, accessToken은 null, 메시지에 실패 원인 포함)
        UserLoginResponseDTO errorResponse = UserLoginResponseDTO.builder()
                .loggedIn(false)
                .accessToken(null)
                .message("로그인 실패: " + exception.getMessage())
                .build();

        // DTO를 JSON 문자열로 변환 후 응답 본문에 작성
        String jsonResponse = objectMapper.writeValueAsString(errorResponse);
        response.getWriter().write(jsonResponse);
    }
}
