package com.example.authservice.controller;

import com.example.authservice.dto.*;
import com.example.authservice.service.TokenService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/auths")
public class TokenController {

    private final TokenService tokenService;

    @PostMapping("/refresh")
    public RefreshTokenResponseDTO refreshToken(@RequestBody RefreshTokenRequestDTO refreshTokenRequestDTO,
                                                HttpServletRequest request,
                                                HttpServletResponse response) {
        return tokenService.refreshToken(refreshTokenRequestDTO.getRefreshToken(),response,request);
    }

    @PostMapping("/validToken")
    public ValidTokenResponseDTO validToken(@RequestBody ValidTokenRequestDTO validTokenRequestDTO) {
        return tokenService.validateToken(validTokenRequestDTO.getToken());
    }
}
