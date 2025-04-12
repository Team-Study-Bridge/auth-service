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

    public NicknameUpdateResponseDTO updateNickname(String accessToken, String nickname) {
        // 토큰 검증 및 사용자 정보 추출
        ClaimsResponseDTO claims = tokenProviderService.getAuthentication(accessToken);
        if (claims == null || claims.getNickname() == null) {
            return NicknameUpdateResponseDTO.builder()
                    .success(false)
                    .message("토큰이 유효하지 않습니다.")
                    .build();
        }
        // 유저를 Redis에서 확인하기
        String userIdStr = redisTemplate.opsForValue().get("accessToken:" + accessToken);
        if (userIdStr == null) {
            return NicknameUpdateResponseDTO.builder()
                    .success(false)
                    .message("Redis에서 유저 정보를 찾을 수 없습니다.")
                    .build();
        }
        Long userId = Long.parseLong(userIdStr);
        System.out.println(userId);

        if (!Validator.validateNickname(nickname)) {
            return NicknameUpdateResponseDTO.builder()
                    .success(false)
                    .message("닉네임은 2~20자이며, 공백이나 특수문자를 포함할 수 없습니다.")
                    .build();
        }
        if (badWordFilter.containsBadWord(nickname)) {
            return NicknameUpdateResponseDTO.builder()
                    .success(false)
                    .message("사용할수 없는 닉네임입니다.")
                    .build();
        }
        try {
            userMapper.updateNickname(userId, nickname);
            return NicknameUpdateResponseDTO.builder()
                    .success(true)
                    .message("닉네임이 성공적으로 변경되었습니다.")
                    .build();
        } catch (Exception e) {
            return NicknameUpdateResponseDTO.builder()
                    .success(false)
                    .message("닉네임 변경 중 오류가 발생했습니다: " + e.getMessage())
                    .build();
        }
    }

    public PasswordUpdateResponseDTO updatePassword(String accessToken, String currentPassword, String newPassword) {
        // 1. 토큰 유효성 확인 및 유저 ID 추출
        ClaimsResponseDTO claims = tokenProviderService.getAuthentication(accessToken);
        if (claims == null || claims.getId() == null) {
            return PasswordUpdateResponseDTO.builder()
                    .success(false)
                    .message("유효하지 않은 토큰입니다.")
                    .build();
        }

        Long userId = claims.getId();
        User user = userMapper.findById(userId);
        if (user == null) {
            return PasswordUpdateResponseDTO.builder()
                    .success(false)
                    .message("사용자 정보를 찾을 수 없습니다.")
                    .build();
        }

        // 2. 현재 비밀번호 일치 확인
        if (!bCryptPasswordEncoder.matches(currentPassword, user.getPassword())) {
            return PasswordUpdateResponseDTO.builder()
                    .success(false)
                    .message("현재 비밀번호가 일치하지 않습니다.")
                    .build();
        }

        // 3. 새 비밀번호 유효성 검사
        if (!Validator.validatePassword(newPassword)) {
            return PasswordUpdateResponseDTO.builder()
                    .success(false)
                    .message("비밀번호는 8~20자 사이여야 하며, 특수문자와 숫자를 포함해야 합니다.")
                    .build();
        }

        // 4. 암호화 후 DB 업데이트
        try {
            String encryptedPassword = bCryptPasswordEncoder.encode(newPassword);
            userMapper.updatePassword(userId, encryptedPassword);
            return PasswordUpdateResponseDTO.builder()
                    .success(true)
                    .message("비밀번호가 성공적으로 변경되었습니다.")
                    .build();
        } catch (Exception e) {
            return PasswordUpdateResponseDTO.builder()
                    .success(false)
                    .message("비밀번호 변경 중 오류가 발생했습니다: " + e.getMessage())
                    .build();
        }
    }

    public DeleteAccountResponseDTO deleteAccount(String accessToken, HttpServletRequest request, HttpServletResponse response) {

        Long userId = tokenProviderService.getAuthentication(accessToken).getId();
        if (userId == null) {
            return DeleteAccountResponseDTO.builder()
                    .success(false)
                    .message("유효하지 않은 토큰입니다.")
                    .build();
        }

        try {
            userMapper.deactivateUser(userId);

            redisTemplate.delete("accessToken:" + userId);
            redisTemplate.delete("refreshToken:" + userId);

            CookieUtil.deleteCookie(request, response, "refreshToken");

            return DeleteAccountResponseDTO.builder()
                    .success(true)
                    .message("계정이 정상적으로 삭제되었습니다.")
                    .build();
        } catch (Exception e) {
            return DeleteAccountResponseDTO.builder()
                    .success(false)
                    .message("계정 삭제 중 오류가 발생했습니다: " + e.getMessage())
                    .build();
        }
    }



}
