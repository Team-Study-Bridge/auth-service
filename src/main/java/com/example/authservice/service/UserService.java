package com.example.authservice.service;

import com.example.authservice.dto.*;
import com.example.authservice.mapper.UserMapper;
import com.example.authservice.model.User;
import com.example.authservice.util.BadWordFilter;
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
    private final BadWordFilter badWordFilter;
    private final BCryptPasswordEncoder bCryptPasswordEncoder;
    private final RedisTemplate<String, String> redisTemplate;
    private final EmailVerificationService emailVerificationService;

    public UserJoinResponseDTO save(UserJoinRequestDTO userJoinRequestDTO, HttpServletResponse response) {

        ValidationResultDTO validationResult = Validator.validateUserInput(
                userJoinRequestDTO.getPassword(),
                userJoinRequestDTO.getNickname()
        );

        if (!validationResult.isSuccess()) {
            return UserJoinResponseDTO.builder()
                    .success(false)
                    .message("검증 실패")
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

            TokenRequestDTO tokenRequestDTO = TokenRequestDTO.builder()
                    .nickname(user.getNickname())
                    .profileImage(user.getProfileImage())
                    .build();

            String accessToken = tokenProviderService.generateToken(tokenRequestDTO, Duration.ofHours(2));
            String refreshToken = tokenProviderService.generateToken(tokenRequestDTO, Duration.ofDays(7));

            redisTemplate.opsForValue().set("accessToken:" + id, accessToken, Duration.ofHours(2));
            redisTemplate.opsForValue().set("refreshToken:" + id, refreshToken, Duration.ofDays(7));

            CookieUtil.addCookie(response, "refreshToken", refreshToken, 7 * 24 * 60 * 60);

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
        TokenRequestDTO tokenRequestDTO = TokenRequestDTO.builder()
                .nickname(user.getNickname())
                .profileImage(user.getProfileImage())
                .build();

        String accessToken = tokenProviderService.generateToken(tokenRequestDTO, Duration.ofHours(2));
        String refreshToken = tokenProviderService.generateToken(tokenRequestDTO, Duration.ofDays(2));

        redisTemplate.opsForValue().set(accessToken, user.getId().toString(), Duration.ofHours(2));

        CookieUtil.addCookie(response, "refreshToken", refreshToken, 7 * 24 * 60 * 60);

        return UserLoginResponseDTO.builder()
                .loggedIn(true)
                .accessToken(accessToken)
                .message("환영합니다 "+ user.getNickname() +" 님")
                .build();
    }

    public UserLoginResponseDTO logout(HttpServletRequest request, HttpServletResponse response) {
        CookieUtil.deleteCookie(request, response, "refreshToken");
        if(CookieUtil.getCookieValue(request,"refreshToken")!=null) {
            return UserLoginResponseDTO.builder()
                    .loggedIn(true)
                    .message("로그아웃에 실패하였습니다.")
                    .build();
        }
        return UserLoginResponseDTO.builder()
                .loggedIn(false)
                .message("정상적으로 로그아웃 처리 되었습니다.")
                .build();
    }

    public NicknameUpdateResponseDTO updateNickname(String nickname) {
        if (badWordFilter.containsBadWord(nickname)) {
            return NicknameUpdateResponseDTO.builder()
                    .success(false)
                    .message("사용하실 수 없는 닉네임입니다.")
                    .build();
        }

        try {
            userMapper.updateNickname(nickname);
            return NicknameUpdateResponseDTO.builder()
                    .success(true)
                    .message("닉네임이 성공적으로 변경되었습니다.")
                    .build();
        } catch (Exception e) {
            return NicknameUpdateResponseDTO.builder()
                    .success(false)
                    .message("닉네임 변경 중 오류가 발생했습니다.")
                    .build();
        }
    }
}
