package com.example.authservice.controller;

import com.example.authservice.dto.RefreshTokenResponseDTO;
import com.example.authservice.dto.ValidTokenResponseDTO;
import com.example.authservice.service.TokenService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/auths")
public class TokenController {

    private final TokenService tokenService;

    @PostMapping("/refresh")
    public ResponseEntity<RefreshTokenResponseDTO> refreshToken(@RequestHeader("Authorization") String accessToken,
                                                                HttpServletRequest request,
                                                                HttpServletResponse response) {
        return tokenService.refreshToken(accessToken, response, request);
    }

    @PostMapping("/valid-token")
    public ValidTokenResponseDTO validToken(@RequestHeader("Authorization") String accessToken) {
        return tokenService.validateToken(accessToken);
    }
}
