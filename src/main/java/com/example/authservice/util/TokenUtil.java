package com.example.authservice.util;

import org.springframework.stereotype.Component;

@Component
public class TokenUtil {

    private final String BEARER_PREFIX = "Bearer ";

    // Authorization 헤더에서 Bearer 제거
    public String cleanBearerToken(String tokenHeader) {
        if (tokenHeader == null) return null;
        if (tokenHeader.toLowerCase().startsWith(BEARER_PREFIX.toLowerCase())) {
            return tokenHeader.substring(BEARER_PREFIX.length());
        }
        return tokenHeader;
    }

    // 순수 토큰만 남는지 검사 (선택적으로 사용 가능)
    public boolean isBearerFormat(String tokenHeader) {
        return tokenHeader != null && tokenHeader.toLowerCase().startsWith(BEARER_PREFIX.toLowerCase());
    }

}