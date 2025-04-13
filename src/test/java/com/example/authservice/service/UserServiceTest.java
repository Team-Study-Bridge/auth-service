package com.example.authservice.service;

import com.example.authservice.dto.ClaimsResponseDTO;
import com.example.authservice.dto.UserJoinRequestDTO;
import com.example.authservice.dto.UserJoinResponseDTO;
import com.example.authservice.dto.UserLoginResponseDTO;
import com.example.authservice.mapper.UserMapper;
import com.example.authservice.model.User;
import com.example.authservice.type.Provider;
import com.example.authservice.type.Role;
import com.example.authservice.type.Status;
import com.example.authservice.util.BadWordFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class UserServiceTest {

    @InjectMocks
    private UserService userService;

    @Mock private UserMapper userMapper;
    @Mock private TokenProviderService tokenProviderService;
    @Mock private BCryptPasswordEncoder bCryptPasswordEncoder;
    @Mock private RedisTemplate<String, Object> redisTemplate;
    @Mock private EmailVerificationService emailVerificationService;
    @Mock private BadWordFilter badWordFilter;
    @Mock private HttpServletResponse response;
    @Mock private HttpServletRequest request;


    @Mock private ValueOperations<String, Object> valueOperations;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    // 1. 회원가입 성공 케이스
    @Test
    void save_회원가입성공() {
        UserJoinRequestDTO requestDTO = UserJoinRequestDTO.builder()
                .email("test@example.com")
                .password("StrongPass123!")
                .nickname("GoodName")
                .profileImage("img.jpg")
                .build();

        when(userMapper.findEmailByEmail(anyString())).thenReturn(null);
        when(emailVerificationService.isVerified(anyString())).thenReturn(true);
        when(badWordFilter.containsBadWord(anyString())).thenReturn(false);

        User user = User.builder()
                .id(1L)
                .email("test@example.com")
                .password("encodedPass")
                .nickname("GoodName")
                .phoneNumber(null)
                .role(Role.STUDENT)
                .provider(Provider.LOCAL)
                .providerId(null)
                .profileImage("img.jpg")
                .status(Status.ACTIVE)
                .lastLogin(null)
                .createdAt(null)
                .isVerified(true)
                .build();
        when(bCryptPasswordEncoder.encode(anyString())).thenReturn("encodedPass");
        when(tokenProviderService.generateToken(any(), any())).thenReturn("access", "refresh");

        doNothing().when(userMapper).insertUser(any());
        doNothing().when(emailVerificationService).deleteEmailVerification(any());

        ResponseEntity<UserJoinResponseDTO> result = userService.save(requestDTO, response);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isNotNull();
        assertThat(result.getBody().isSuccess()).isTrue();
    }
    @Test
    void forceLogin_유저없음() {
        when(userMapper.findByEmail("notfound@example.com")).thenReturn(null);

        ResponseEntity<UserLoginResponseDTO> user =
                userService.forceLogin("notfound@example.com", "anyPassword", response);

        assertThat(user.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(user.getBody().isLoggedIn()).isFalse();
        assertThat(user.getBody().getMessage()).contains("이메일 또는 비밀번호가 일치하지 않습니다.");
    }

    @Test
    void forceLogin_비밀번호틀림() {
        String rawPassword = "pass";
        String encodedPassword = new BCryptPasswordEncoder().encode("otherPassword");

        User user = User.builder()
                .id(1L)
                .email("test@example.com")
                .password(encodedPassword)
                .nickname("Tester")
                .role(Role.STUDENT)
                .provider(Provider.LOCAL)
                .status(Status.ACTIVE)
                .profileImage("img.jpg")
                .isVerified(true)
                .build();

        when(userMapper.findByEmail("test@example.com")).thenReturn(user);
        when(bCryptPasswordEncoder.matches("pass", encodedPassword)).thenReturn(false);

        ResponseEntity<UserLoginResponseDTO> response1 = userService.forceLogin("test@example.com", "pass", response);

        assertThat(response1.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response1.getBody().isLoggedIn()).isFalse();
        assertThat(response1.getBody().getMessage()).contains("이메일 또는 비밀번호가 일치하지 않습니다.");
    }

    @Test
    void save_회원가입중예외발생() {
        UserJoinRequestDTO requestDTO = UserJoinRequestDTO.builder()
                .email("test@example.com")
                .password("StrongPass123!")
                .nickname("GoodName")
                .build();

        when(userMapper.findEmailByEmail(anyString())).thenReturn(null);
        when(emailVerificationService.isVerified(anyString())).thenReturn(true);
        when(badWordFilter.containsBadWord(anyString())).thenReturn(false);

        when(bCryptPasswordEncoder.encode(anyString())).thenReturn("encodedPass");

        // ❗ 예외 발생 설정
        doThrow(new RuntimeException("DB 오류!")).when(userMapper).insertUser(any());

        ResponseEntity<UserJoinResponseDTO> result = userService.save(requestDTO, response);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(result.getBody().isSuccess()).isFalse();
        assertThat(result.getBody().getMessage()).contains("회원가입 중 오류가 발생했습니다");
    }




    @Test
    void login_비밀번호틀림() {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String rawPassword = "correctPassword";
        String encodedPassword = encoder.encode(rawPassword);

        User user = User.builder()
                .id(1L)
                .email("test@example.com")
                .password(encodedPassword)
                .nickname("Tester")
                .role(Role.STUDENT)
                .provider(Provider.LOCAL)
                .status(Status.ACTIVE)
                .profileImage("img.jpg")
                .isVerified(true)
                .build();

        when(userMapper.findByEmail("test@example.com")).thenReturn(user);
        when(bCryptPasswordEncoder.matches("wrongPassword", encodedPassword)).thenReturn(false);

        ResponseEntity<UserLoginResponseDTO> response = userService.login("test@example.com", "wrongPassword", this.response);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isLoggedIn()).isFalse();
    }



    // 2. 이미 가입된 이메일
    @Test
    void save_이메일중복() {
        UserJoinRequestDTO requestDTO = UserJoinRequestDTO.builder()
                .email("test@example.com") // ✅ null이 아니게 설정!
                .password("whatever")
                .nickname("tester")
                .build();
        when(userMapper.findEmailByEmail(anyString())).thenReturn("test@example.com");

        ResponseEntity<UserJoinResponseDTO> result = userService.save(requestDTO, response);
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(result.getBody().getMessage()).contains("이미 가입된 이메일");
    }

    // 3. 유효하지 않은 닉네임 or 패스워드
    @Test
    void save_검증실패() {
        UserJoinRequestDTO requestDTO = UserJoinRequestDTO.builder()
                .email("valid@example.com")
                .password("short")
                .nickname("okay")
                .build();

        when(userMapper.findEmailByEmail(anyString())).thenReturn(null);

        ResponseEntity<UserJoinResponseDTO> result = userService.save(requestDTO, response);
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(result.getBody().isSuccess()).isFalse();
    }

    // 4. 금칙어 닉네임
    @Test
    void save_금칙어() {
        UserJoinRequestDTO requestDTO = UserJoinRequestDTO.builder()
                .email("valid@example.com")
                .password("Strong123!")
                .nickname("badword")
                .build();

        when(userMapper.findEmailByEmail(anyString())).thenReturn(null);
        when(badWordFilter.containsBadWord(anyString())).thenReturn(true);

        ResponseEntity<UserJoinResponseDTO> result = userService.save(requestDTO, response);
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(result.getBody().getMessage()).contains("사용할 수 없는 닉네임");
    }

    // 5. 인증되지 않은 이메일
    @Test
    void save_미인증이메일() {
        UserJoinRequestDTO requestDTO = UserJoinRequestDTO.builder()
                .email("valid@example.com")
                .password("Strong123!")
                .nickname("GoodName")
                .build();

        when(userMapper.findEmailByEmail(anyString())).thenReturn(null);
        when(badWordFilter.containsBadWord(anyString())).thenReturn(false);
        when(emailVerificationService.isVerified(anyString())).thenReturn(false);

        ResponseEntity<UserJoinResponseDTO> result = userService.save(requestDTO, response);
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    // 6. 로그인 성공
    @Test
    void login_성공() {
        User user = User.builder()
                .id(1L)
                .email("test@example.com")
                .password("encodedPass")
                .nickname("GoodName")
                .phoneNumber(null)
                .role(Role.STUDENT)
                .provider(Provider.LOCAL)
                .providerId(null)
                .profileImage("img.jpg")
                .status(Status.ACTIVE)
                .lastLogin(null)
                .createdAt(null)
                .isVerified(true)
                .build();

        when(userMapper.findByEmail(anyString())).thenReturn(user);
        when(bCryptPasswordEncoder.matches(anyString(), anyString())).thenReturn(true);
        when(redisTemplate.opsForValue().get("accessToken:" + user.getId())).thenReturn(null);
        when(tokenProviderService.generateToken(any(), any())).thenReturn("access", "refresh");

        ResponseEntity<UserLoginResponseDTO> result = userService.login("test@example.com", "pass", response);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody().isLoggedIn()).isTrue();
    }

    // 7. 로그인 실패 (비밀번호 불일치)
    @Test
    void login_실패() {
        when(userMapper.findByEmail(anyString())).thenReturn(null);

        ResponseEntity<UserLoginResponseDTO> result = userService.login("nope@example.com", "pass", response);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(result.getBody().isLoggedIn()).isFalse();
    }

    // 8. 로그인 실패 (중복 로그인 감지)
    @Test
    void login_중복로그인() {
        User user = User.builder()
                .id(1L)
                .email("test@example.com")
                .password("encodedPass")
                .nickname("GoodName")
                .phoneNumber(null)
                .role(Role.STUDENT)
                .provider(Provider.LOCAL)
                .providerId(null)
                .profileImage("img.jpg")
                .status(Status.ACTIVE)
                .lastLogin(null)
                .createdAt(null)
                .isVerified(true)
                .build();

        when(userMapper.findByEmail(anyString())).thenReturn(user);
        when(bCryptPasswordEncoder.matches(anyString(), anyString())).thenReturn(true);
        when(redisTemplate.opsForValue().get("accessToken:" + user.getId())).thenReturn("someToken");

        ResponseEntity<UserLoginResponseDTO> result = userService.login("test@example.com", "pass", response);
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    // 9. forceLogin 정상
    @Test
    void forceLogin_성공() {
        User user = User.builder()
                .id(1L)
                .email("test@example.com")
                .password("encodedPass")
                .nickname("GoodName")
                .phoneNumber(null)
                .role(Role.STUDENT)
                .provider(Provider.LOCAL)
                .providerId(null)
                .profileImage("img.jpg")
                .status(Status.ACTIVE)
                .lastLogin(null)
                .createdAt(null)
                .isVerified(true)
                .build();

        when(userMapper.findByEmail(anyString())).thenReturn(user);
        when(bCryptPasswordEncoder.matches(anyString(), anyString())).thenReturn(true);
        when(tokenProviderService.generateToken(any(), any())).thenReturn("access", "refresh");

        ResponseEntity<UserLoginResponseDTO> result = userService.forceLogin("test@example.com", "pass", response);
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    // 10. logout 정상
    @Test
    void logout_정상() {
        when(tokenProviderService.getAuthentication(anyString())).thenReturn(
                ClaimsResponseDTO.builder().id(1L).build()
        );

        ResponseEntity<UserLoginResponseDTO> result = userService.logout("access", request, response);
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody().isLoggedIn()).isFalse();
    }

    // 11. logout 실패
    @Test
    void logout_실패() {
        when(tokenProviderService.getAuthentication(anyString())).thenThrow(new RuntimeException("Invalid"));

        ResponseEntity<UserLoginResponseDTO> result = userService.logout("access", request, response);
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(result.getBody().isLoggedIn()).isTrue();
    }
}
