package com.example.authservice.service;

import com.example.authservice.config.security.CustomUserDetails;
import com.example.authservice.dto.UserJoinResponseDTO;
import com.example.authservice.dto.UserLoginResponseDTO;
import com.example.authservice.mapper.UserMapper;
import com.example.authservice.model.User;
import com.example.authservice.util.CookieUtil;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserMapper userMapper;
    private final AuthenticationManager authenticationManager;
    private final TokenProviderService tokenProviderService;

    public UserJoinResponseDTO save(User user) {
        userMapper.insertUser(user);
        try {
            return UserJoinResponseDTO.builder()
                    .isSuccess(true)
                    .message("User has been saved successfully")
                    .build();
        } catch (Exception e) {
            return UserJoinResponseDTO.builder()
                    .isSuccess(false)
                    .message(e.getMessage())
                    .build();
        }

    }

    public UserLoginResponseDTO login(String email, String password,
                                      HttpServletResponse response) {
            Authentication authenticate = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(email, password));

            SecurityContextHolder.getContext().setAuthentication(authenticate);

            User user = ((CustomUserDetails) authenticate.getPrincipal()).getUser();
            String accessToken = tokenProviderService.generateToken(user, Duration.ofHours(2));
            String refreshToken = tokenProviderService.generateToken(user, Duration.ofDays(2));
            CookieUtil.addCookie(response, "refreshToken", refreshToken, 7 * 24 * 60 * 60);

            return UserLoginResponseDTO.builder()
                    .loggedIn(true)
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .email(user.getEmail())
                    .nickname(user.getNickname())
                    .build();
    }

}
