package com.example.authservice.service;

import com.example.authservice.dto.*;
import com.example.authservice.mapper.UserMapper;
import com.example.authservice.model.User;
import com.example.authservice.type.Status;
import com.example.authservice.util.BadWordFilter;
import com.example.authservice.util.CookieUtil;
import com.example.authservice.util.TokenUtil;
import com.example.authservice.util.Validator;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserMapper userMapper;
    private final TokenProviderService tokenProviderService;
    private final BCryptPasswordEncoder bCryptPasswordEncoder;
    private final RedisTemplate<String, String> redisTemplate;
    private final EmailVerificationService emailVerificationService;
    private final BadWordFilter badWordFilter;
    private final S3Service s3Service;
    private final TokenUtil tokenUtil;

    public ResponseEntity<UserJoinResponseDTO> save(UserJoinRequestDTO userJoinRequestDTO, MultipartFile profileImage, HttpServletResponse response) {
        ValidationResultDTO validationResult = Validator.validateUserInput(
                userJoinRequestDTO.getPassword(),
                userJoinRequestDTO.getNickname()
        );

        String userEmail = userMapper.findEmailByEmail(userJoinRequestDTO.getEmail());
        if (userEmail != null) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(
                    UserJoinResponseDTO.builder()
                            .success(false)
                            .message("이미 가입된 이메일입니다.")
                            .build()
            );
        }

        if (!validationResult.isSuccess()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    UserJoinResponseDTO.builder()
                            .success(false)
                            .message("패스워드 또는 닉네임 설정이 잘못됐습니다.")
                            .errors(validationResult)
                            .build()
            );
        }

        if (badWordFilter.containsBadWord(userJoinRequestDTO.getNickname())) {
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(
                    UserJoinResponseDTO.builder()
                            .success(false)
                            .message("사용할 수 없는 닉네임입니다.")
                            .build()
            );
        }

        if (!emailVerificationService.isVerified(userJoinRequestDTO.getEmail())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                    UserJoinResponseDTO.builder()
                            .success(false)
                            .message("이메일 인증이 완료되지 않았습니다.")
                            .build()
            );
        }

        String imageUrl = null;
        if (profileImage != null && !profileImage.isEmpty()) {
            try {
                imageUrl = s3Service.upload(profileImage, "profile");
            } catch (IOException e) {
                log.error("프로필 이미지 업로드 실패: {}", e.getMessage());
                return ResponseEntity.internalServerError().body(
                        UserJoinResponseDTO.builder()
                                .success(false)
                                .message("프로필 이미지 업로드에 실패했습니다.")
                                .build()
                );
            }
        }
        System.out.println("imageUrl" + imageUrl);

        userJoinRequestDTO.setProfileImage(imageUrl);
        User user = userJoinRequestDTO.toUser(bCryptPasswordEncoder);

        System.out.println(user.getProfileImage());

        try {
            userMapper.insertUser(user);
            Long id = user.getId();

            ClaimsRequestDTO claimsRequestDTO = ClaimsRequestDTO.builder()
                    .userId(id)
                    .nickname(user.getNickname())
                    .profileImage(user.getProfileImage())
                    .build();
            System.out.println(claimsRequestDTO.getProfileImage());
            String accessToken = tokenProviderService.generateToken(claimsRequestDTO, Duration.ofHours(2));
            String refreshToken = tokenProviderService.generateToken(claimsRequestDTO, Duration.ofDays(7));

            redisTemplate.opsForValue().set("accessToken:" + id, accessToken, Duration.ofHours(2));
            redisTemplate.opsForValue().set("refreshToken:" + id, refreshToken, Duration.ofDays(7));

            CookieUtil.addCookie(response, "refreshToken", refreshToken, 7 * 24 * 60 * 60);
            emailVerificationService.deleteEmailVerification(user.getEmail());

            return ResponseEntity.ok(
                    UserJoinResponseDTO.builder()
                            .success(true)
                            .accessToken(accessToken)
                            .message("회원가입 및 로그인 완료")
                            .build()
            );
        } catch (Exception e) {
            log.error("회원가입 중 오류 발생: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    UserJoinResponseDTO.builder()
                            .success(false)
                            .message("회원가입 중 오류가 발생했습니다: " + e.getMessage())
                            .build()
            );
        }
    }

    public ResponseEntity<UserLoginResponseDTO> login(String email, String password, HttpServletResponse response) {
        User user = userMapper.findByEmail(email);

        // 1. 유저 존재 여부 및 비밀번호 체크
        if (user == null || !bCryptPasswordEncoder.matches(password, user.getPassword())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                    UserLoginResponseDTO.builder()
                            .loggedIn(false)
                            .message("이메일 또는 비밀번호가 일치하지 않습니다.")
                            .build()
            );
        }

        if (user.getStatus() != Status.ACTIVE) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                    UserLoginResponseDTO.builder()
                            .loggedIn(false)
                            .message("활동 정지된 계정입니다.")
                            .build()
            );
        }

        // 기존 로그인 세션 감지
        String existingAccessToken = redisTemplate.opsForValue().get("accessToken:" + user.getId());
        if (existingAccessToken != null) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(
                    UserLoginResponseDTO.builder()
                            .loggedIn(false)
                            .message("현재 계정은 다른 브라우저에서 로그인 중입니다.\n계속 진행하시겠습니까?\n\n(로그인 시 기존 로그인된 계정은 로그아웃 됩니다.)")
                            .build()
            );
        }


        ClaimsRequestDTO claimsRequestDTO = ClaimsRequestDTO.builder()
                .userId(user.getId())
                .nickname(user.getNickname())
                .profileImage(user.getProfileImage())
                .build();

        String accessToken = tokenProviderService.generateToken(claimsRequestDTO, Duration.ofHours(2));
        String refreshToken = tokenProviderService.generateToken(claimsRequestDTO, Duration.ofDays(7));

        redisTemplate.opsForValue().set("accessToken:" + user.getId(), accessToken, Duration.ofHours(2));
        redisTemplate.opsForValue().set("refreshToken:" + user.getId(), refreshToken, Duration.ofDays(7));

        CookieUtil.addCookie(response, "refreshToken", refreshToken, 7 * 24 * 60 * 60);

        return ResponseEntity.ok(
                UserLoginResponseDTO.builder()
                        .loggedIn(true)
                        .accessToken(accessToken)
                        .message("환영합니다 " + user.getNickname() + " 님")
                        .build()
        );
    }

    public ResponseEntity<UserLoginResponseDTO> forceLogin(String email, String password, HttpServletResponse response) {
        User user = userMapper.findByEmail(email);

        if (user == null || !bCryptPasswordEncoder.matches(password, user.getPassword())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                    UserLoginResponseDTO.builder()
                            .loggedIn(false)
                            .message("이메일 또는 비밀번호가 일치하지 않습니다.")
                            .build()
            );
        }

        ClaimsRequestDTO claimsRequestDTO = ClaimsRequestDTO.builder()
                .userId(user.getId())
                .nickname(user.getNickname())
                .profileImage(user.getProfileImage())
                .build();

        String accessToken = tokenProviderService.generateToken(claimsRequestDTO, Duration.ofHours(2));
        String refreshToken = tokenProviderService.generateToken(claimsRequestDTO, Duration.ofDays(7));

        redisTemplate.opsForValue().set("accessToken:" + user.getId(), accessToken, Duration.ofHours(2));
        redisTemplate.opsForValue().set("refreshToken:" + user.getId(), refreshToken, Duration.ofDays(7));

        CookieUtil.addCookie(response, "refreshToken", refreshToken, 7 * 24 * 60 * 60);

        return ResponseEntity.ok(
                UserLoginResponseDTO.builder()
                        .loggedIn(true)
                        .accessToken(accessToken)
                        .message("환영합니다 " + user.getNickname() + " 님")
                        .build()
        );
    }

    public ResponseEntity<UserLoginResponseDTO> logout(String accessToken, HttpServletRequest request, HttpServletResponse response) {
    try {
        String cleanBearerToken = tokenUtil.cleanBearerToken(accessToken);
        Long id = tokenProviderService.getAuthentication(cleanBearerToken).getId();
        redisTemplate.delete("accessToken:" + id);
        redisTemplate.delete("refreshToken:" + id);

        CookieUtil.deleteCookie(request, response, "refreshToken");
        System.out.println("cookie: " + request.getCookies().toString());
        return ResponseEntity.ok(
                UserLoginResponseDTO.builder()
                        .loggedIn(false)
                        .message("정상적으로 로그아웃 되었습니다.")
                        .build()
        );
    } catch (Exception e) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                UserLoginResponseDTO.builder()
                        .loggedIn(true)
                        .message("로그아웃 실패: " + e.getMessage())
                        .build()
        );
    }
    }


}
