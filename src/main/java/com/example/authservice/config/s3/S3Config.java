package com.example.authservice.config.s3;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("!test")
@RequiredArgsConstructor
public class S3Config {

    private final AwsProperties awsProperties;

    @Bean
    public AmazonS3 amazonS3() {
        BasicAWSCredentials credentials = new BasicAWSCredentials(
                awsProperties.getCredentials().getAccessKey(),
                awsProperties.getCredentials().getSecretKey()
        );
        return AmazonS3ClientBuilder.standard()
                .withRegion(awsProperties.getRegion().getStatic_())
                .withCredentials(new AWSStaticCredentialsProvider(credentials))
                .build();
    }
}
