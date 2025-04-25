package com.example.authservice.service;

import com.example.authservice.dto.*;
import com.example.authservice.exception.ImageSizeExceededException;
import com.example.authservice.exception.InvalidImageExtensionException;
import com.example.authservice.mapper.UserMapper;
import com.example.authservice.model.User;
import com.example.authservice.type.FileType;
import com.example.authservice.type.Role;
import com.example.authservice.util.BadWordFilter;
import com.example.authservice.util.CookieUtil;
import com.example.authservice.util.TokenUtil;
import com.example.authservice.util.Validator;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserApiService {

    private final TokenProviderService tokenProviderService;
    private final BadWordFilter badWordFilter;
    private final UserMapper userMapper;
    private final RedisTemplate<String, String> redisTemplate;
    private final BCryptPasswordEncoder bCryptPasswordEncoder;
    private final S3Service s3Service;
    private final TokenUtil tokenUtil;

    public ResponseEntity<NicknameUpdateResponseDTO> updateNickname(String accessToken, String nickname) {
        String cleanBearerToken = tokenUtil.cleanBearerToken(accessToken);
        ClaimsResponseDTO claims = tokenProviderService.getAuthentication(cleanBearerToken);
        Long userId = claims.getId();

        String savedToken = redisTemplate.opsForValue().get("accessToken:" + userId);
        if (savedToken == null || !savedToken.equals(cleanBearerToken)) {
            return ResponseEntity.status(HttpServletResponse.SC_UNAUTHORIZED).body(
                    NicknameUpdateResponseDTO.builder()
                            .success(false)
                            .message("유효하지 않은 토큰입니다.")
                            .build()
            );
        }

        if (!Validator.validateNickname(nickname)) {
            return ResponseEntity.badRequest().body(
                    NicknameUpdateResponseDTO.builder()
                            .success(false)
                            .message("닉네임은 2~10자이며, 공백이나 특수문자를 포함할 수 없습니다.")
                            .build()
            );
        }

        if (badWordFilter.containsBadWord(nickname)) {
            return ResponseEntity.unprocessableEntity().body(
                    NicknameUpdateResponseDTO.builder()
                            .success(false)
                            .message("사용할 수 없는 닉네임입니다.")
                            .build()
            );
        }

        try {
            userMapper.updateNickname(userId, nickname);

            User updatedUser = userMapper.findById(userId);
            ClaimsRequestDTO newClaims = ClaimsRequestDTO.builder()
                    .userId(userId)
                    .nickname(updatedUser.getNickname())
                    .profileImage(updatedUser.getProfileImage())
                    .build();

            String newAccessToken = tokenProviderService.generateToken(newClaims, Duration.ofHours(2));
            redisTemplate.opsForValue().set("accessToken:" + userId, newAccessToken, Duration.ofHours(2));

            return ResponseEntity.ok(
                    NicknameUpdateResponseDTO.builder()
                            .success(true)
                            .message("닉네임이 성공적으로 변경되었습니다.")
                            .nickname(nickname)
                            .accessToken(newAccessToken)
                            .build()
            );
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(
                    NicknameUpdateResponseDTO.builder()
                            .success(false)
                            .message("닉네임 변경 중 오류가 발생했습니다: " + e.getMessage())
                            .build()
            );
        }
    }

    public ResponseEntity<PasswordUpdateResponseDTO> updatePassword(String accessToken, String currentPassword, String newPassword) {
        String cleanBearerToken = tokenUtil.cleanBearerToken(accessToken);
        ClaimsResponseDTO claims = tokenProviderService.getAuthentication(cleanBearerToken);
        Long userId = claims.getId();

        String savedToken = redisTemplate.opsForValue().get("accessToken:" + userId);
        if (savedToken == null || !savedToken.equals(cleanBearerToken)) {
            return ResponseEntity.status(HttpServletResponse.SC_UNAUTHORIZED).body(
                    PasswordUpdateResponseDTO.builder()
                            .success(false)
                            .message("유효하지 않은 토큰입니다.")
                            .build()
            );
        }

        User user = userMapper.findById(userId);
        if (user == null) {
            return ResponseEntity.badRequest().body(
                    PasswordUpdateResponseDTO.builder()
                            .success(false)
                            .message("사용자 정보를 찾을 수 없습니다.")
                            .build()
            );
        }

        if (!bCryptPasswordEncoder.matches(currentPassword, user.getPassword())) {
            return ResponseEntity.badRequest().body(
                    PasswordUpdateResponseDTO.builder()
                            .success(false)
                            .message("현재 비밀번호가 일치하지 않습니다.")
                            .build()
            );
        }

        if (!Validator.validatePassword(newPassword)) {
            return ResponseEntity.badRequest().body(
                    PasswordUpdateResponseDTO.builder()
                            .success(false)
                            .message("비밀번호는 8~20자 사이여야 하며, 특수문자와 숫자를 포함해야 합니다.")
                            .build()
            );
        }

        try {
            String encodedPassword = bCryptPasswordEncoder.encode(newPassword);
            userMapper.updatePassword(userId, encodedPassword);

            return ResponseEntity.ok(
                    PasswordUpdateResponseDTO.builder()
                            .success(true)
                            .message("비밀번호가 성공적으로 변경되었습니다.")
                            .build()
            );
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(
                    PasswordUpdateResponseDTO.builder()
                            .success(false)
                            .message("비밀번호 변경 중 오류가 발생했습니다: " + e.getMessage())
                            .build()
            );
        }
    }

    public ResponseEntity<DeleteAccountResponseDTO> deleteAccount(String accessToken, HttpServletRequest request, HttpServletResponse response) {
        String cleanBearerToken = tokenUtil.cleanBearerToken(accessToken);
        ClaimsResponseDTO claims = tokenProviderService.getAuthentication(cleanBearerToken);
        Long userId = claims.getId();

        String savedToken = redisTemplate.opsForValue().get("accessToken:" + userId);
        if (savedToken == null || !savedToken.equals(cleanBearerToken)) {
            return ResponseEntity.status(HttpServletResponse.SC_UNAUTHORIZED).body(
                    DeleteAccountResponseDTO.builder()
                            .success(false)
                            .message("유효하지 않은 토큰입니다.")
                            .build()
            );
        }
        try {
            // 계정 비활성화 처리
            userMapper.deactivateUser(userId);

            // Redis에서 토큰 삭제
            redisTemplate.delete("accessToken:" + userId);
            redisTemplate.delete("refreshToken:" + userId);

            // 쿠키 제거
            CookieUtil.deleteCookie(request, response, "refreshToken");

            return ResponseEntity.ok(
                    DeleteAccountResponseDTO.builder()
                            .success(true)
                            .message("계정이 정상적으로 삭제되었습니다.")
                            .build()
            );
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(
                    DeleteAccountResponseDTO.builder()
                            .success(false)
                            .message("계정 삭제 중 오류가 발생했습니다: " + e.getMessage())
                            .build()
            );
        }
    }

    public ResponseEntity<ProfileImageUpdateResponseDTO> updateProfileImage(
            String accessToken,
            MultipartFile profileImage
    ) {
        try {
            String cleanBearerToken = tokenUtil.cleanBearerToken(accessToken);
            ClaimsResponseDTO claims = tokenProviderService.getAuthentication(cleanBearerToken);
            Long userId = claims.getId();

            String savedToken = redisTemplate.opsForValue().get("accessToken:" + userId);
            if (savedToken == null || !savedToken.equals(cleanBearerToken)) {
                return ResponseEntity.status(HttpServletResponse.SC_UNAUTHORIZED).body(
                        ProfileImageUpdateResponseDTO.builder()
                                .success(false)
                                .message("유효하지 않은 액세스 토큰입니다.")
                                .build()
                );
            }

            if (profileImage == null || profileImage.isEmpty()) {
                return ResponseEntity.badRequest().body(
                        ProfileImageUpdateResponseDTO.builder()
                                .success(false)
                                .message("변경하실 프로필 이미지를 등록해주세요.")
                                .build()
                );
            }

            String oldImageUrl = userMapper.findProfileImageById(userId);
            if (oldImageUrl != null && !oldImageUrl.isBlank()) {
                s3Service.delete(oldImageUrl);
            }

            String imageUrl = s3Service.upload(profileImage, FileType.IMAGE);
            userMapper.updateProfileImage(userId, imageUrl);

            User updatedUser = userMapper.findById(userId);
            ClaimsRequestDTO newClaims = ClaimsRequestDTO.builder()
                    .userId(userId)
                    .nickname(updatedUser.getNickname())
                    .profileImage(updatedUser.getProfileImage())
                    .build();

            String newAccessToken = tokenProviderService.generateToken(newClaims, Duration.ofHours(2));
            redisTemplate.opsForValue().set("accessToken:" + userId, newAccessToken, Duration.ofHours(2));

            return ResponseEntity.ok(
                    ProfileImageUpdateResponseDTO.builder()
                            .success(true)
                            .message("프로필 이미지가 성공적으로 업데이트되었습니다.")
                            .profileImage(imageUrl)
                            .accessToken(newAccessToken)
                            .build()
            );

        } catch (ImageSizeExceededException e) {
            return ResponseEntity.badRequest().body(
                    ProfileImageUpdateResponseDTO.builder()
                            .success(false)
                            .message("이미지는 최대 1MB까지만 업로드할 수 있습니다.")
                            .build()
            );
        } catch (InvalidImageExtensionException e) {
            return ResponseEntity.badRequest().body(
                    ProfileImageUpdateResponseDTO.builder()
                            .success(false)
                            .message("지원하지 않는 이미지 확장자입니다.")
                            .build()
            );
        } catch (IOException e) {
            log.error("S3 업로드 IOException", e);
            return ResponseEntity.internalServerError().body(
                    ProfileImageUpdateResponseDTO.builder()
                            .success(false)
                            .message("이미지 업로드 중 서버 오류가 발생했습니다.")
                            .build()
            );
        } catch (Exception e) {
            log.error("프로필 이미지 업데이트 전체 실패", e);
            return ResponseEntity.internalServerError().body(
                    ProfileImageUpdateResponseDTO.builder()
                            .success(false)
                            .message("서버 오류로 인해 이미지 업데이트에 실패했습니다.")
                            .build()
            );
        }
    }


    public ResponseEntity<UserInfoResponseDTO> userInfo(String accessToken) {
        try {
            String cleanBearerToken = tokenUtil.cleanBearerToken(accessToken);
            ClaimsResponseDTO claims = tokenProviderService.getAuthentication(cleanBearerToken);
            Long userId = claims.getId();

            String savedToken = redisTemplate.opsForValue().get("accessToken:" + userId);
            if (savedToken == null || !savedToken.equals(cleanBearerToken)) {
                return ResponseEntity.status(HttpServletResponse.SC_UNAUTHORIZED).body(
                        UserInfoResponseDTO.builder()
                                .success(false)
                                .message("유효하지 않은 토큰입니다.")
                                .build()
                );
            }

            User user = userMapper.findById(userId);
            if (user == null) {
                return ResponseEntity.badRequest().body(
                        UserInfoResponseDTO.builder()
                                .success(false)
                                .message("사용자 정보를 찾을 수 없습니다.")
                                .build()
                );
            }

            return ResponseEntity.ok(
                    UserInfoResponseDTO.builder()
                            .success(true)
                            .message("사용자의 정보를 정상적으로 불러왔습니다.")
                            .role(user.getRole())
                            .email(user.getEmail())
                            .phoneNumber(user.getPhoneNumber())
                            .nickname(user.getNickname())
                            .profileImage(user.getProfileImage())
                            .provider(user.getProvider())
                            .status(user.getStatus())
                            .build()
            );

        } catch (Exception e) {
            log.error("유저 정보 조회 중 예외 발생", e);
            return ResponseEntity.internalServerError().body(
                    UserInfoResponseDTO.builder()
                            .success(false)
                            .message("서버 오류로 사용자 정보를 불러오지 못했습니다.")
                            .build()
            );
        }
    }

    public ResponseEntity<UserInfoRoleResponseDTO> userInfoRole(String accessToken) {
        try {
        String cleanBearerToken = tokenUtil.cleanBearerToken(accessToken);
        ClaimsResponseDTO claims = tokenProviderService.getAuthentication(cleanBearerToken);
        Long userId = claims.getId();

        String savedToken = redisTemplate.opsForValue().get("accessToken:" + userId);
        if (savedToken == null || !savedToken.equals(cleanBearerToken)) {
            return ResponseEntity.status(HttpServletResponse.SC_UNAUTHORIZED).body(
                    UserInfoRoleResponseDTO.builder()
                            .success(false)
                            .message("유효하지 않은 토큰입니다.")
                            .build()
            );
        }

        Role userRole = userMapper.findRoleById(userId);
        if (userRole == null) {
            return ResponseEntity.badRequest().body(
                    UserInfoRoleResponseDTO.builder()
                            .success(false)
                            .message("사용자 정보를 찾을 수 없습니다.")
                            .build()
            );
        }

        return ResponseEntity.ok(
                UserInfoRoleResponseDTO.builder()
                        .success(true)
                        .message("사용자의 정보를 정상적으로 불러왔습니다.")
                        .role(userRole)
                        .build()
        );

        } catch (Exception e) {
            log.error("유저 정보 조회 중 예외 발생", e);
            return ResponseEntity.internalServerError().body(
                    UserInfoRoleResponseDTO.builder()
                            .success(false)
                            .message("서버 오류로 사용자 정보를 불러오지 못했습니다.")
                            .build()
            );
        }
    }
}