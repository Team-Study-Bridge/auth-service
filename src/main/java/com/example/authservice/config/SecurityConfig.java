package com.example.authservice.config;

import com.example.authservice.config.jwt.JwtAuthenticationFilter;
import com.example.authservice.config.security.oauth.CustomOAuth2AuthenticationFailureHandler;
import com.example.authservice.config.security.oauth.CustomOAuth2AuthenticationSuccessHandler;
import com.example.authservice.service.CustomOAuth2UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

import static org.springframework.security.config.Customizer.withDefaults;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomOAuth2UserService customOAuth2UserService;
    private final CustomOAuth2AuthenticationSuccessHandler customOAuth2AuthenticationSuccessHandler;
    private final CustomOAuth2AuthenticationFailureHandler customOAuth2AuthenticationFailureHandler;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public BCryptPasswordEncoder bCryptPasswordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // CSRF 비활성화. JWT 기반 인증이면 CSRF 보호가 불필요하거나 별도 처리
                .csrf(AbstractHttpConfigurer::disable)
                // CORS 설정 적용
                .cors(withDefaults())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/swagger-ui.html",
                                "/swagger-ui/**",
                                "/v3/api-docs/**",
                                "/swagger-resources/**",
                                "/webjars/**"
                        ).permitAll()
                        // 인증 없이 접근 허용하는 URL 설정
                        .requestMatchers("/auths/**").permitAll()
                        .requestMatchers("/api/**").permitAll()
                        .requestMatchers("/oauth2/**").permitAll()
                        .requestMatchers("/login/**").permitAll()
                        .requestMatchers("/auths/join").permitAll()

                        // 그 외 요청은 인증 필요
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                // OAuth2 로그인 설정 추가
                .oauth2Login(oauth2 -> oauth2
                        // 사용자 정보를 가져올 때 커스텀 OAuth2UserService를 사용
                        .userInfoEndpoint(userInfo -> userInfo
                                .userService(customOAuth2UserService)
                        )
                        // OAuth2 인증 성공 후 customSuccessHandler에서 refreshToken을 쿠키에 저장하도록 처리
                        .successHandler(customOAuth2AuthenticationSuccessHandler)
                        // 인증 실패 시 처리
                        .failureHandler(customOAuth2AuthenticationFailureHandler)
                )
                // 폼 로그인 비활성화 (JWT 인증을 사용하는 경우 불필요)
                .formLogin(AbstractHttpConfigurer::disable);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        // 프론트엔드 도메인 설정 (배포 시에는 실제 도메인으로 변경)
        configuration.setAllowedOrigins(Arrays.asList("http://localhost:9000"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type"));
        // 쿠키 전송 허용 (JWT 인증 시 필요)
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        return source;
    }
}
