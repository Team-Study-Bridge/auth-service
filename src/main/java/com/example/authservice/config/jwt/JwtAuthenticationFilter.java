package com.example.authservice.config.jwt;

import com.example.authservice.dto.ApiErrorResponseDTO;
import com.example.authservice.dto.ClaimsResponseDTO;
import com.example.authservice.mapper.UserMapper;
import com.example.authservice.service.TokenProviderService;
import com.example.authservice.type.Status;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final UserMapper userMapper;
    private final TokenProviderService tokenProviderService;
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper(); // JSON 변환용
    //..
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        String uri = request.getRequestURI();
        return uri.matches("^/api/teachers/\\d+/profile$")
                || uri.matches("^/auths/users/\\d+/email$")
                || uri.matches("^/auths/users/by-type$");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String token = extractToken(request);
        if (token == null) {
            filterChain.doFilter(request, response);
            return;
        }

        // 1. JWT 유효성 검사
        int result = tokenProviderService.validateToken(token);
        if (result == 3) {
            setErrorResponse(response, 40103, "비정상적인 접근입니다.");
            return;
        }

        if (result == 2) {
            setErrorResponse(response, 40102, "토큰이 만료되었습니다.");
            return;
        }

        // 2. Claims 추출 (한 번만 호출)
        ClaimsResponseDTO claims = tokenProviderService.getAuthentication(token);
        Long userId = claims.getId();

        // 3. Redis에 저장된 토큰과 비교
        String savedToken = String.valueOf(redisTemplate.opsForValue().get("accessToken:" + userId));
        if (savedToken == null) {
            setErrorResponse(response, 40102, "토큰이 만료되었습니다.");
            return;
        }

        if (!savedToken.equals(token)) {
            setErrorResponse(response, 40104, "다른 기기에서 로그인되었습니다.");
            return;
        }

        // 4. 계정 상태 확인
        Status status = userMapper.findStatusById(userId);
        if (status != Status.ACTIVE) {
            setErrorResponse(response, 40301, "비활성화된 계정입니다. 관리자에게 문의하세요.");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header == null || !header.toLowerCase().startsWith("bearer ")) {
            return null;
        }
        return header.substring(7); // Bearer 제거
    }

    private void setErrorResponse(HttpServletResponse response, int code, String message) throws IOException {
        ApiErrorResponseDTO errorResponse = ApiErrorResponseDTO.builder()
                .success(false)
                .code(code)
                .message(message)
                .build();

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }
}
