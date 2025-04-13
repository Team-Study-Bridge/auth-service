package com.example.authservice.controller;

import com.example.authservice.config.security.oauth.CustomOAuth2User;
import com.example.authservice.dto.OAuthUserInfoResponseDTO;
import com.example.authservice.model.User;
import com.example.authservice.service.SocialUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/oauth2")
@RequiredArgsConstructor
public class SocialUserController {
    private final SocialUserService socialUserService;

    @GetMapping("/info")
    public ResponseEntity<OAuthUserInfoResponseDTO> getUserInfo(@AuthenticationPrincipal CustomOAuth2User customOAuth2User) {
        return socialUserService.getUserInfo(customOAuth2User);
    }

    @PostMapping("/link")
    public ResponseEntity<OAuthUserInfoResponseDTO> linkAccount(@AuthenticationPrincipal CustomOAuth2User customUser) {
        return socialUserService.linkAccount(customUser);
    }
}
