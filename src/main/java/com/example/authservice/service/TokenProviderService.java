package com.example.authservice.service;

import com.example.authservice.config.jwt.JwtProperties;
import com.example.authservice.dto.ClaimsResponseDTO;
import com.example.authservice.model.User;
import com.example.authservice.type.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Duration;
import java.util.Base64;
import java.util.Date;
import java.util.List;

import static io.jsonwebtoken.Header.JWT_TYPE;
import static io.jsonwebtoken.Header.TYPE;
import static io.jsonwebtoken.SignatureAlgorithm.HS512;

@Slf4j
@Service
@RequiredArgsConstructor
public class TokenProviderService {

    private final JwtProperties jwtProperties;

    public String generateToken(User user, Duration expiration) {
        Date now = new Date();
        return makeToken(
                new Date(now.getTime() + expiration.toMillis()),
                user
        );
    }

    public int validToken(String token) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(getSecretKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            return 1; // 유효한 토큰
        } catch (ExpiredJwtException e) {
            log.info("Expired JWT token");
            return 2; // 만료된 토큰
        } catch (Exception e) {
            log.info("Invalid JWT token");
            return 3; // 잘못된 토큰
        }
    }

    public ClaimsResponseDTO getAuthentication(String token) {
        Claims claims = getClaims(token);
        return ClaimsResponseDTO.builder()
                .email(claims.getSubject())
                .roles(List.of(claims.get("role", String.class)))
                .build();
    }

    public User getTokenDetails(String token) {
        Claims claims = getClaims(token);

        return User.builder()
                .id(claims.get("id", Long.class))
                .email(claims.getSubject())
                .nickname(claims.get("nickname", String.class))
                .role(Role.valueOf(claims.get("role", String.class)))
                .provider(null) // 토큰에 포함되지 않으므로 null로 설정
                .providerId(null)
                .profileImage(null)
                .status(null)
                .lastLogin(null)
                .createdAt(null)
                .isVerified(claims.get("isVerified", Boolean.class))
                .build();
    }

    private Claims getClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSecretKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    private String makeToken(Date expire, User user) {
        Date now = new Date();

        return Jwts.builder()
                .setHeaderParam(TYPE, JWT_TYPE)
                .setIssuer(jwtProperties.getIssuer())
                .setIssuedAt(now)
                .setExpiration(expire)
                .setSubject(user.getEmail())
                .claim("nickname", user.getNickname())
                .claim("role", user.getRole().name())
                .signWith(getSecretKey(), HS512)
                .compact();
    }

    private SecretKey getSecretKey() {
        byte[] keyBytes = Base64.getDecoder().decode(jwtProperties.getSecretKey());
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
