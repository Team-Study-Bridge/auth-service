package com.example.authservice.service;

import com.example.authservice.dto.*;
import com.example.authservice.mapper.UserMapper;
import com.example.authservice.model.User;
import com.example.authservice.type.*;
import com.example.authservice.util.BadWordFilter;
import com.example.authservice.util.CookieUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class UserApiServiceTest {

    @InjectMocks
    private UserApiService userApiService;

    @Mock private TokenProviderService tokenProviderService;
    @Mock private BadWordFilter badWordFilter;
    @Mock private UserMapper userMapper;
    @Mock private RedisTemplate<String, String> redisTemplate;
    @Mock private BCryptPasswordEncoder bCryptPasswordEncoder;
    @Mock private HttpServletRequest request;
    @Mock private HttpServletResponse response;
    @Mock private ValueOperations<String, String> valueOperations;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    // ========== updateNickname ==========

    @Test
    void updateNickname_성공() {
        String token = "validAccessToken";
        String nickname = "NewNick";

        when(tokenProviderService.getAuthentication(token)).thenReturn(
                ClaimsResponseDTO.builder().nickname("OldNick").build()
        );
        when(valueOperations.get("accessToken:" + token)).thenReturn("1");
        when(badWordFilter.containsBadWord(nickname)).thenReturn(false);

        NicknameUpdateResponseDTO result = userApiService.updateNickname(token, nickname);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getMessage()).contains("변경");
        verify(userMapper).updateNickname(1L, nickname);
    }

    @Test
    void updateNickname_닉네임null() {
        String token = "token";

        // claims는 존재하지만 닉네임은 null
        ClaimsResponseDTO claims = ClaimsResponseDTO.builder().nickname(null).build();
        when(tokenProviderService.getAuthentication(token)).thenReturn(claims);

        NicknameUpdateResponseDTO result = userApiService.updateNickname(token, "whatever");

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getMessage()).contains("토큰이 유효하지 않습니다");
    }

    @Test
    void updateNickname_토큰없음() {
        when(tokenProviderService.getAuthentication("badToken")).thenReturn(null);

        NicknameUpdateResponseDTO result = userApiService.updateNickname("badToken", "Nick");
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getMessage()).contains("토큰");
    }

    @Test
    void updateNickname_레디스값없음() {
        when(tokenProviderService.getAuthentication("token")).thenReturn(
                ClaimsResponseDTO.builder().nickname("Old").build()
        );
        when(valueOperations.get(anyString())).thenReturn(null);

        NicknameUpdateResponseDTO result = userApiService.updateNickname("token", "Nick");
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getMessage()).contains("Redis");
    }

    @Test
    void updateNickname_닉네임검증실패() {
        when(tokenProviderService.getAuthentication("token")).thenReturn(
                ClaimsResponseDTO.builder().nickname("Old").build()
        );
        when(valueOperations.get(anyString())).thenReturn("1");

        NicknameUpdateResponseDTO result = userApiService.updateNickname("token", "!");
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getMessage()).contains("닉네임은");
    }
    @Test
    void updatePassword_claims_id_null() {
        String token = "token";
        ClaimsResponseDTO claims = ClaimsResponseDTO.builder().id(null).build();
        when(tokenProviderService.getAuthentication(token)).thenReturn(claims);

        PasswordUpdateResponseDTO result = userApiService.updatePassword(token, "old", "new");
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getMessage()).contains("유효하지 않은 토큰");
    }


    @Test
    void updateNickname_금칙어() {
        when(tokenProviderService.getAuthentication("token")).thenReturn(
                ClaimsResponseDTO.builder().nickname("Old").build()
        );
        when(valueOperations.get(anyString())).thenReturn("1");
        when(badWordFilter.containsBadWord("bad")).thenReturn(true);

        NicknameUpdateResponseDTO result = userApiService.updateNickname("token", "bad");
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getMessage()).contains("사용할수 없는 닉네임");
    }

    @Test
    void updateNickname_DB오류() {
        when(tokenProviderService.getAuthentication("token")).thenReturn(
                ClaimsResponseDTO.builder().nickname("Old").build()
        );
        when(valueOperations.get(anyString())).thenReturn("1");
        when(badWordFilter.containsBadWord("GoodNick")).thenReturn(false);
        doThrow(new RuntimeException("DB error")).when(userMapper).updateNickname(anyLong(), anyString());

        NicknameUpdateResponseDTO result = userApiService.updateNickname("token", "GoodNick");
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getMessage()).contains("오류");
    }

    // ========== updatePassword ==========

    @Test
    void updatePassword_성공() {
        when(tokenProviderService.getAuthentication("token"))
                .thenReturn(ClaimsResponseDTO.builder().id(1L).build());

        User user = User.builder().password("encodedOld").build();
        when(userMapper.findById(1L)).thenReturn(user);
        when(bCryptPasswordEncoder.matches("oldPass", "encodedOld")).thenReturn(true);
        when(bCryptPasswordEncoder.encode("newPass123!")).thenReturn("encodedNew");

        PasswordUpdateResponseDTO result = userApiService.updatePassword("token", "oldPass", "newPass123!");
        assertThat(result.isSuccess()).isTrue();
    }

    @Test
    void updatePassword_토큰없음() {
        when(tokenProviderService.getAuthentication("token")).thenReturn(null);

        PasswordUpdateResponseDTO result = userApiService.updatePassword("token", "old", "new");
        assertThat(result.isSuccess()).isFalse();
    }

    @Test
    void updatePassword_유저없음() {
        when(tokenProviderService.getAuthentication("token"))
                .thenReturn(ClaimsResponseDTO.builder().id(1L).build());

        when(userMapper.findById(1L)).thenReturn(null);

        PasswordUpdateResponseDTO result = userApiService.updatePassword("token", "old", "new");
        assertThat(result.isSuccess()).isFalse();
    }

    @Test
    void updatePassword_현재비밀번호불일치() {
        when(tokenProviderService.getAuthentication("token"))
                .thenReturn(ClaimsResponseDTO.builder().id(1L).build());

        when(userMapper.findById(1L)).thenReturn(User.builder().password("encoded").build());
        when(bCryptPasswordEncoder.matches("wrong", "encoded")).thenReturn(false);

        PasswordUpdateResponseDTO result = userApiService.updatePassword("token", "wrong", "new");
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getMessage()).contains("현재 비밀번호가");
    }

    @Test
    void updatePassword_새비밀번호검증실패() {
        when(tokenProviderService.getAuthentication("token"))
                .thenReturn(ClaimsResponseDTO.builder().id(1L).build());

        when(userMapper.findById(1L)).thenReturn(User.builder().password("encoded").build());
        when(bCryptPasswordEncoder.matches(anyString(), anyString())).thenReturn(true);

        PasswordUpdateResponseDTO result = userApiService.updatePassword("token", "old", "짧음");
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getMessage()).contains("비밀번호는 8~20자");
    }

    @Test
    void updatePassword_DB오류() {
        when(tokenProviderService.getAuthentication("token"))
                .thenReturn(ClaimsResponseDTO.builder().id(1L).build());

        when(userMapper.findById(1L)).thenReturn(User.builder().password("encoded").build());
        when(bCryptPasswordEncoder.matches(anyString(), anyString())).thenReturn(true);
        when(bCryptPasswordEncoder.encode(anyString())).thenReturn("encodedNew");
        doThrow(new RuntimeException("DB Fail")).when(userMapper).updatePassword(anyLong(), anyString());

        PasswordUpdateResponseDTO result = userApiService.updatePassword("token", "old", "NewPassword1!");
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getMessage()).contains("오류");
    }

    // ========== deleteAccount ==========

    @Test
    void deleteAccount_성공() {
        when(tokenProviderService.getAuthentication("token"))
                .thenReturn(ClaimsResponseDTO.builder().id(1L).build());

        DeleteAccountResponseDTO result = userApiService.deleteAccount("token", request, response);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getMessage()).contains("정상적으로 삭제");
    }

    @Test
    void deleteAccount_토큰유효X() {
        when(tokenProviderService.getAuthentication("token"))
                .thenReturn(ClaimsResponseDTO.builder().id(null).build());

        DeleteAccountResponseDTO result = userApiService.deleteAccount("token", request, response);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getMessage()).contains("유효하지 않은 토큰");
    }

    @Test
    void deleteAccount_DB오류() {
        when(tokenProviderService.getAuthentication("token"))
                .thenReturn(ClaimsResponseDTO.builder().id(1L).build());

        doThrow(new RuntimeException("DB fail")).when(userMapper).deactivateUser(anyLong());

        DeleteAccountResponseDTO result = userApiService.deleteAccount("token", request, response);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getMessage()).contains("오류");
    }
}
