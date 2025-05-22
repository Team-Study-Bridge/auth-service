package com.example.authservice.testsupport;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.AnonymousAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.example.authservice.dto.SendCodeResponseDTO;
import com.example.authservice.dto.VerifyCodeResponseDTO;
import com.example.authservice.service.EmailVerificationService;
import io.findify.s3mock.S3Mock;
import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.javamail.JavaMailSender;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@org.springframework.boot.test.context.TestConfiguration(proxyBeanMethods = false)
@Profile("test")
public class TestConfiguration {

    // Redis 설정
    @Bean
    @Primary
    public RedisConnectionFactory redisConnectionFactory() {
        return new LettuceConnectionFactory("localhost", 6379);
    }

    @Bean
    @Primary
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());

        template.afterPropertiesSet();
        return template;
    }

    // S3 Mock 설정
    @Bean
    public S3Mock s3Mock() {
        return new S3Mock.Builder().withPort(8001).withInMemoryBackend().build();
    }

    @Bean
    @Primary
    public AmazonS3 amazonS3(S3Mock s3Mock) {
        s3Mock.start();
        AmazonS3 client = AmazonS3ClientBuilder
                .standard()
                .withPathStyleAccessEnabled(true)
                .withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration("http://localhost:8001", "ap-northeast-2"))
                .withCredentials(new AWSStaticCredentialsProvider(new AnonymousAWSCredentials()))
                .build();

        if (!client.doesBucketExistV2("dummy-bucket")) {
            client.createBucket("dummy-bucket");
        }
        return client;
    }

    @Bean
    @Primary
    public JavaMailSender javaMailSender() {
        return Mockito.mock(JavaMailSender.class);
    }

    @Bean
    @Primary
    public EmailVerificationService emailVerificationService() {
        EmailVerificationService mock = Mockito.mock(EmailVerificationService.class);

        when(mock.isVerified(anyString())).thenReturn(true);

        when(mock.verifyCode(anyString(), anyString())).thenReturn(
                ResponseEntity.ok(
                        VerifyCodeResponseDTO.builder()
                                .success(true)
                                .message("인증이 완료되었습니다.")
                                .build()
                )
        );

        when(mock.sendVerificationCode(anyString())).thenReturn(
                ResponseEntity.ok(
                        SendCodeResponseDTO.builder()
                                .success(true)
                                .message("인증 코드가 발송되었습니다.")
                                .build()
                )
        );

        return mock;
    }
}