package com.example.authservice.service;

import com.example.authservice.dto.ClaimsResponseDTO;
import com.example.authservice.dto.DeleteAccountResponseDTO;
import com.example.authservice.dto.NicknameUpdateResponseDTO;
import com.example.authservice.dto.PasswordUpdateResponseDTO;
import com.example.authservice.mapper.UserMapper;
import com.example.authservice.model.User;
import com.example.authservice.util.BadWordFilter;
import com.example.authservice.util.CookieUtil;
import com.example.authservice.util.Validator;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserApiService {

    private final TokenProviderService tokenProviderService;
    private final BadWordFilter badWordFilter;
    private final UserMapper userMapper;
    private final RedisTemplate<String, String> redisTemplate;
    private final BCryptPasswordEncoder bCryptPasswordEncoder;

    public ResponseEntity<NicknameUpdateResponseDTO> updateNickname(String accessToken, String nickname) {
        ClaimsResponseDTO claims = tokenProviderService.getAuthentication(accessToken);
        Long userId = claims.getId();

        // Redis에 저장된 토큰과 비교
        String savedToken = redisTemplate.opsForValue().get("accessToken:" + userId);
        System.out.println("savedToken: " + savedToken);
        System.out.println(savedToken.equals(accessToken));
        System.out.println(accessToken);
        if (savedToken == null || !savedToken.equals(accessToken)) {
            return ResponseEntity.status(HttpServletResponse.SC_UNAUTHORIZED).body(
                    NicknameUpdateResponseDTO.builder()
                            .success(false)
                            .message("유효하지 않은 토큰입니다.")
                            .build()
            );
        }

        // 닉네임 유효성 검사
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
            return ResponseEntity.ok(
                    NicknameUpdateResponseDTO.builder()
                            .success(true)
                            .message("닉네임이 성공적으로 변경되었습니다.")
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
        ClaimsResponseDTO claims = tokenProviderService.getAuthentication(accessToken);
        Long userId = claims.getId();

        String savedToken = redisTemplate.opsForValue().get("accessToken:" + userId);
        if (savedToken == null || !savedToken.equals(accessToken)) {
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
        ClaimsResponseDTO claims = tokenProviderService.getAuthentication(accessToken);
        Long userId = claims.getId();

        String savedToken = redisTemplate.opsForValue().get("accessToken:" + userId);
        if (savedToken == null || !savedToken.equals(accessToken)) {
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
}

