package com.example.authservice.service;

import com.example.authservice.config.security.oauth.CustomOAuth2User;
import com.example.authservice.dto.ClaimsRequestDTO;
import com.example.authservice.dto.OAuthUserInfoResponseDTO;
import com.example.authservice.mapper.OAuth2UserMapper;
import com.example.authservice.model.User;
import com.example.authservice.type.Provider;
import com.example.authservice.type.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.ResponseEntity;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class SocialUserServiceTest {

    @Mock
    private OAuth2UserMapper oAuth2UserMapper;

    @Mock
    private TokenProviderService tokenProviderService;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @InjectMocks
    private SocialUserService socialUserService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        // RedisTemplate 체이닝 대응
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    void getUserInfo_WhenUserIsNull_ReturnsUnauthorized() {
        ResponseEntity<OAuthUserInfoResponseDTO> response = socialUserService.getUserInfo(null);

        assertThat(response.getStatusCodeValue()).isEqualTo(401);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isFalse();
        assertThat(response.getBody().getMessage()).isEqualTo("Unauthorized");
    }

    @Test
    void getUserInfo_WhenNeedsLinking_ReturnsResponseWithNeedsLinkingTrue() {
        User user = createUser(Provider.LOCAL);
        CustomOAuth2User customUser = new CustomOAuth2User(user, "token123", "refresh123");
        customUser.setNeedsLinking(true);

        ResponseEntity<OAuthUserInfoResponseDTO> response = socialUserService.getUserInfo(customUser);

        assertThat(response.getStatusCodeValue()).isEqualTo(200);
        assertThat(response.getBody().isSuccess()).isFalse();
        assertThat(response.getBody().isNeedsLinking()).isTrue();
        assertThat(response.getBody().getMessage()).isEqualTo("기존 계정이 존재합니다. 연동하시겠습니까?");
    }

    @Test
    void getUserInfo_WhenLinkedAlready_ReturnsSuccess() {
        User user = createUser(Provider.NAVER);
        CustomOAuth2User customUser = new CustomOAuth2User(user, "token456", "refresh456");
        customUser.setNeedsLinking(false);

        ResponseEntity<OAuthUserInfoResponseDTO> response = socialUserService.getUserInfo(customUser);

        assertThat(response.getStatusCodeValue()).isEqualTo(200);
        assertThat(response.getBody().isSuccess()).isTrue();
        assertThat(response.getBody().isNeedsLinking()).isFalse();
        assertThat(response.getBody().getMessage()).isEqualTo("로그인 성공");
    }

    @Test
    void linkAccount_WhenAlreadyLinked_ReturnsBadRequest() {
        User user = createUser(Provider.NAVER); // 이미 연동됨
        CustomOAuth2User customUser = new CustomOAuth2User(user, "any", "any");

        ResponseEntity<OAuthUserInfoResponseDTO> response = socialUserService.linkAccount(customUser);

        assertThat(response.getStatusCodeValue()).isEqualTo(400);
        assertThat(response.getBody().isSuccess()).isFalse();
        assertThat(response.getBody().getMessage()).isEqualTo("이미 연동된 계정입니다.");
    }

    @Test
    void linkAccount_WhenNotLinked_SuccessfullyLinksAccount() {
        User user = createUser(Provider.LOCAL);
        user.setId(1L);
        user.setProviderId("naver123");

        // ✅ 진짜 유저 객체에 provider 설정
        user.setProvider(Provider.NAVER);

        CustomOAuth2User customUser = new CustomOAuth2User(user, null, null);

        String accessToken = "access-token";
        String refreshToken = "refresh-token";

        when(tokenProviderService.generateToken(any(ClaimsRequestDTO.class), eq(Duration.ofHours(2)))).thenReturn(accessToken);
        when(tokenProviderService.generateToken(any(ClaimsRequestDTO.class), eq(Duration.ofDays(7)))).thenReturn(refreshToken);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        ResponseEntity<OAuthUserInfoResponseDTO> response = socialUserService.linkAccount(customUser);

        verify(oAuth2UserMapper).updateUserWithSocialInfo(user.getId(), user.getProviderId(), Provider.NAVER);
        verify(valueOperations, atLeastOnce()).set(anyString(), any(), any());

        assertThat(response.getStatusCodeValue()).isEqualTo(200);
        assertThat(response.getBody().isSuccess()).isTrue();
    }


    private User createUser(Provider provider) {
        return User.builder()
                .id(1L)
                .nickname("tester")
                .email("test@example.com")
                .profileImage("image.png")
                .provider(provider)
                .providerId("provider-id")
                .role(Role.STUDENT)
                .build();
    }
}
