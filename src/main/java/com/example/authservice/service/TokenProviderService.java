package com.example.authservice.service;

import com.example.authservice.config.jwt.JwtProperties;
import com.example.authservice.dto.ClaimsRequestDTO;
import com.example.authservice.dto.ClaimsResponseDTO;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Duration;
import java.util.Base64;
import java.util.Date;

import static io.jsonwebtoken.Header.JWT_TYPE;
import static io.jsonwebtoken.Header.TYPE;
import static io.jsonwebtoken.SignatureAlgorithm.HS512;

@Service
@RequiredArgsConstructor
public class TokenProviderService {

    private final JwtProperties jwtProperties;

    public String generateToken(ClaimsRequestDTO claimsRequestDTO, Duration expiration) {
        Date now = new Date();
        Date expireDate = new Date(now.getTime() + expiration.toMillis());

        return Jwts.builder()
                .setHeaderParam(TYPE, JWT_TYPE)
                .setIssuer(jwtProperties.getIssuer())
                .setIssuedAt(now)
                .setExpiration(expireDate)
                .setSubject(String.valueOf(claimsRequestDTO.getUserId()))
                .claim("nickname", claimsRequestDTO.getNickname())
                .claim("profileImage", claimsRequestDTO.getProfileImage() != null ? claimsRequestDTO.getProfileImage() : null)
                .signWith(getSecretKey(), HS512)
                .compact();
    }

    public int validateToken(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(getSecretKey()).build().parseClaimsJws(token);
            return 1;
        } catch (ExpiredJwtException e) {
            return 2;
        } catch (Exception e) {
            return 3;
        }
    }

    public ClaimsResponseDTO getAuthentication(String token) {
        Claims claims = getClaims(token);

        return ClaimsResponseDTO.builder()
                .id(Long.valueOf(claims.getSubject()))
                .nickname(claims.get("nickname", String.class))
                .profileImage(claims.get("profileImage", String.class))
                .build();
    }

    private Claims getClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSecretKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    private SecretKey getSecretKey() {
        byte[] keyBytes = Base64.getDecoder().decode(jwtProperties.getSecretKey());
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
