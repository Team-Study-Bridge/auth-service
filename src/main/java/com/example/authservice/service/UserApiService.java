package com.example.authservice.service;

import com.example.authservice.dto.ClaimsResponseDTO;
import com.example.authservice.dto.NicknameUpdateResponseDTO;
import com.example.authservice.mapper.UserMapper;
import com.example.authservice.util.BadWordFilter;
import com.example.authservice.util.Validator;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserApiService {

    private final TokenProviderService tokenProviderService;
    private RedisTemplate<Object, Object> redisTemplate;
    private final BadWordFilter badWordFilter;
    private final UserMapper userMapper;

    public NicknameUpdateResponseDTO updateNickname(String nickname, String accessToken) {
        // 토큰 검증 및 사용자 정보 추출
        ClaimsResponseDTO claims = tokenProviderService.getAuthentication(accessToken);

        if (claims == null || claims.getNickname() == null) {
            return NicknameUpdateResponseDTO.builder()
                    .success(false)
                    .message("토큰이 유효하지 않습니다.")
                    .build();
        }

        // 유저를 Redis에서 확인하기
        String userIdStr = String.valueOf(redisTemplate.opsForValue().get("accessToken" + accessToken));
        Long userId = userIdStr != null ? Long.parseLong(userIdStr) : null;

        if (userId == null) {
            return NicknameUpdateResponseDTO.builder()
                    .success(false)
                    .message("인증되지 않은 사용자입니다.")
                    .build();
        }
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
}
