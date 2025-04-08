package com.example.authservice.service;

import com.example.authservice.dto.*;
import com.example.authservice.mapper.UserMapper;
import com.example.authservice.model.User;
import com.example.authservice.util.CookieUtil;
import com.example.authservice.util.Validator;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserMapper userMapper;
    private final TokenProviderService tokenProviderService;
    private final BCryptPasswordEncoder bCryptPasswordEncoder;
    private final RedisTemplate<String, Object> redisTemplate;
    private final EmailVerificationService emailVerificationService;

    public UserJoinResponseDTO save(UserJoinRequestDTO userJoinRequestDTO, HttpServletResponse response) {

        ValidationResultDTO validationResult = Validator.validateUserInput(
                userJoinRequestDTO.getPassword(),
                userJoinRequestDTO.getNickname()
        );

        if (!validationResult.isSuccess()) {
            return UserJoinResponseDTO.builder()
                    .success(false)
                    .message("패스워드 또는 닉네임설정이 잘못됐습니다.")
                    .errors(validationResult)
                    .build();
        }

        if (!emailVerificationService.isVerified(userJoinRequestDTO.getEmail())) {
            return UserJoinResponseDTO.builder()
                    .success(false)
                    .message("이메일 인증이 완료되지 않았습니다. 인증 후 다시 시도하세요.")
                    .build();
        }

        User user = userJoinRequestDTO.toUser(bCryptPasswordEncoder);

        try {
            userMapper.insertUser(user);
            Long id = user.getId();

            ClaimsRequestDTO claimsRequestDTO = ClaimsRequestDTO.builder()
                    .nickname(user.getNickname())
                    .profileImage(user.getProfileImage())
                    .build();

            String accessToken = tokenProviderService.generateToken(claimsRequestDTO, Duration.ofHours(2));
            String refreshToken = tokenProviderService.generateToken(claimsRequestDTO, Duration.ofDays(7));

            redisTemplate.opsForValue().set("accessToken:" + accessToken, String.valueOf(id), Duration.ofHours(2));
            redisTemplate.opsForValue().set("refreshToken:" + refreshToken, String.valueOf(id), Duration.ofDays(7));

            CookieUtil.addCookie(response, "refreshToken", refreshToken, 7 * 24 * 60 * 60);
            emailVerificationService.deleteEmailVerification(user.getEmail());

            return UserJoinResponseDTO.builder()
                    .success(true)
                    .accessToken(accessToken)
                    .message("회원가입 및 로그인 완료")
                    .build();
        } catch (Exception e) {
            log.error("회원가입 중 오류 발생: {}", e.getMessage());
            return UserJoinResponseDTO.builder()
                    .success(false)
                    .message("회원가입 중 오류가 발생했습니다: " + e.getMessage())
                    .build();
        }
    }

    public UserLoginResponseDTO login(String email, String password, HttpServletResponse response) {

        User user = userMapper.findByEmail(email);

        if (user == null) {
            return UserLoginResponseDTO.builder()
                    .loggedIn(false)
                    .message("사용자를 찾을 수 없습니다.")
                    .build();
        }

        if (!bCryptPasswordEncoder.matches(password, user.getPassword())) {
            // 비밀번호가 일치하지 않는 경우 (로그인 실패)
            return UserLoginResponseDTO.builder()
                    .loggedIn(false)
                    .message("비밀번호가 일치하지 않습니다.")
                    .build();
        }

        //  로그인 성공 - JWT 토큰 발급 (Redis에 저장할 userId 포함)
        ClaimsRequestDTO claimsRequestDTO = ClaimsRequestDTO.builder()
                .nickname(user.getNickname())
                .profileImage(user.getProfileImage())
                .build();

        String accessToken = tokenProviderService.generateToken(claimsRequestDTO, Duration.ofHours(2));
        String refreshToken = tokenProviderService.generateToken(claimsRequestDTO, Duration.ofDays(2));

        redisTemplate.opsForValue().set("accessToken:" + accessToken, String.valueOf(user.getId()), Duration.ofHours(2));
        redisTemplate.opsForValue().set("refreshToken:" + refreshToken, String.valueOf(user.getId()), Duration.ofDays(7));

        CookieUtil.addCookie(response, "refreshToken", refreshToken, 7 * 24 * 60 * 60);

        return UserLoginResponseDTO.builder()
                .loggedIn(true)
                .accessToken(accessToken)
                .message("환영합니다 "+ user.getNickname() +" 님")
                .build();
    }

    public UserLoginResponseDTO logout(String accessToken, HttpServletRequest request, HttpServletResponse response) {
        // 쿠키에서 refreshToken 가져오기
        String refreshToken = CookieUtil.getCookieValue(request, "refreshToken");

        if (refreshToken == null) {
            return UserLoginResponseDTO.builder()
                    .loggedIn(false)
                    .message("로그아웃 되었습니다.")
                    .build();
        }

        redisTemplate.delete("accessToken:" + accessToken);
        redisTemplate.delete("refreshToken:" + refreshToken);

        CookieUtil.deleteCookie(request, response, "refreshToken");

        if (CookieUtil.getCookieValue(request, "refreshToken") != null) {
            return UserLoginResponseDTO.builder()
                    .loggedIn(true)
                    .message("로그아웃에 실패하였습니다. (쿠키 삭제 실패)")
                    .build();
        }

        return UserLoginResponseDTO.builder()
                .loggedIn(false)
                .message("정상적으로 로그아웃 되었습니다.")
                .build();
    }
}
