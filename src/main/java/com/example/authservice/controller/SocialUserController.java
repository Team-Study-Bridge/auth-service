package com.example.authservice.controller;

import com.example.authservice.dto.OAuthCodeRequestDTO;
import com.example.authservice.dto.OAuthLoginResponseDTO;
import com.example.authservice.dto.OAuthUserInfoResponseDTO;
import com.example.authservice.service.OAuthService;
import com.example.authservice.type.Provider;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "소셜로그인", description = "소셜 로그인 관련 API")
@RestController
@RequestMapping("/auths/oauth")
@RequiredArgsConstructor
public class SocialUserController {
    private final OAuthService oAuthService;

    @PostMapping("/token")
    public ResponseEntity<OAuthLoginResponseDTO> exchangeCode(@RequestBody OAuthCodeRequestDTO request,
                                                              HttpServletResponse response) {
        return oAuthService.loginWithNaverCode(request.getCode(), request.getState(), response);
    }

    @PostMapping("/link")
    public ResponseEntity<OAuthUserInfoResponseDTO> linkAccount(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam("provider") Provider provider,
            HttpServletResponse response
    ) {
        return oAuthService.linkAccount(authHeader, provider, response);
    }


}
