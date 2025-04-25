package com.example.authservice.config.social;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties("naver")
@Data
public class NaverOAuthProperties {
    private String clientId;
    private String clientSecret;
    private String redirectUri;
}