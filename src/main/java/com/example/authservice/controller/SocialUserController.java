package com.example.authservice.controller;

import com.example.authservice.config.security.CustomOAuth2User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/auths")
@RequiredArgsConstructor
public class SocialUserController {

    @GetMapping("/info")
    public ResponseEntity<?> getUserInfo(@AuthenticationPrincipal CustomOAuth2User customOAuth2User) {
        if (customOAuth2User == null) {
            return ResponseEntity.status(401).body(Map.of("success", false, "message", "Unauthorized"));
        }

        return ResponseEntity.ok(Map.of(
                "success", true,
                "token", customOAuth2User.getJwtToken(),
                "user", customOAuth2User.getAttributes()
        ));
    }

}
