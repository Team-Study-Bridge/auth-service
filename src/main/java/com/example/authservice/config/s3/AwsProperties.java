package com.example.authservice.config.s3;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "spring.cloud.aws")
@Data
public class AwsProperties {
    private Credentials credentials;
    private Region region;
    @Data public static class Credentials {
        private String accessKey;
        private String secretKey;
    }
    @Data public static class Region {
        private String static_; // 'static'이 예약어라서 언더스코어 붙임
    }
}