package com.example.authservice.controller;

import com.example.authservice.dto.UserJoinRequestDTO;
import com.example.authservice.dto.UserLoginRequestDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class UserControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private final String testEmail = "testuser@example.com";
    private final String testPassword = "Password123!";
    private final String testNickname = "통합테스트유저";

    @BeforeEach
    void setUp() throws Exception {
        // 회원가입 사전 수행 (로그인 테스트를 위해)
        UserJoinRequestDTO user = UserJoinRequestDTO.builder()
                .email(testEmail)
                .password(testPassword)
                .nickname(testNickname)
                .build();

        MockMultipartFile userPart = new MockMultipartFile(
                "user", "", "application/json",
                objectMapper.writeValueAsBytes(user)
        );

        MockMultipartFile profileImage = new MockMultipartFile(
                "profileImage", "test.jpg", "image/jpeg", "test".getBytes()
        );

        mockMvc.perform(multipart("/auths/join")
                        .file(userPart)
                        .file(profileImage)
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("회원가입 성공")
    void join_success() throws Exception {
        // 중복 가입 실패 테스트
        UserJoinRequestDTO user = UserJoinRequestDTO.builder()
                .email(testEmail)
                .password(testPassword)
                .nickname("중복닉네임")
                .build();

        MockMultipartFile userPart = new MockMultipartFile(
                "user", "", "application/json",
                objectMapper.writeValueAsBytes(user)
        );

        mockMvc.perform(multipart("/auths/join")
                        .file(userPart)
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("로그인 성공")
    void login_success() throws Exception {
        UserLoginRequestDTO login = UserLoginRequestDTO.builder()
                .email(testEmail)
                .password(testPassword)
                .build();

        mockMvc.perform(post("/auths/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.loggedIn").value(true));
    }

    @Test
    @DisplayName("로그인 실패 - 비밀번호 오류")
    void login_wrongPassword() throws Exception {
        UserLoginRequestDTO login = UserLoginRequestDTO.builder()
                .email(testEmail)
                .password("wrongPassword")
                .build();

        mockMvc.perform(post("/auths/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("강제 로그인 성공")
    void forceLogin_success() throws Exception {
        UserLoginRequestDTO login = UserLoginRequestDTO.builder()
                .email(testEmail)
                .password(testPassword)
                .build();

        mockMvc.perform(post("/auths/force-login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.loggedIn").value(true));
    }

    @Test
    @DisplayName("로그아웃 실패 - 잘못된 토큰")
    void logout_invalidToken() throws Exception {
        mockMvc.perform(delete("/auths/logout")
                        .header("Authorization", "Bearer invalid.token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("유저 이메일 조회")
    void getEmailByUserId_success() throws Exception {
        mockMvc.perform(get("/auths/users/1/email"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("유저 타입 조회 - ALL")
    void getUsersByType_all() throws Exception {
        mockMvc.perform(get("/auths/users/by-type?type=ALL"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("유저 타입 조회 - STUDENTS")
    void getUsersByType_students() throws Exception {
        mockMvc.perform(get("/auths/users/by-type?type=STUDENTS"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("유저 타입 조회 - INSTRUCTORS")
    void getUsersByType_instructors() throws Exception {
        mockMvc.perform(get("/auths/users/by-type?type=INSTRUCTORS"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("유저 타입 조회 - 잘못된 타입")
    void getUsersByType_invalid() throws Exception {
        mockMvc.perform(get("/auths/users/by-type?type=UNKNOWN"))
                .andExpect(status().isOk()); // 빈 리스트 반환 처리됨
    }

    @Test
    @DisplayName("회원가입 실패 - 이메일 인증 안됨")
    void join_emailNotVerified() throws Exception {
        UserJoinRequestDTO user = UserJoinRequestDTO.builder()
                .email("unverified@example.com")
                .password("Valid1234!")
                .nickname("테스트유저")
                .build();

        MockMultipartFile userPart = new MockMultipartFile(
                "user", "", "application/json",
                objectMapper.writeValueAsBytes(user)
        );

        mockMvc.perform(multipart("/auths/join")
                        .file(userPart)
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("회원가입 실패 - 금지어 포함")
    void join_nicknameContainsBadWord() throws Exception {
        UserJoinRequestDTO user = UserJoinRequestDTO.builder()
                .email("badword@example.com")
                .password("Valid1234!")
                .nickname("씨발") // 금지어 예시
                .build();

        MockMultipartFile userPart = new MockMultipartFile(
                "user", "", "application/json",
                objectMapper.writeValueAsBytes(user)
        );

        mockMvc.perform(multipart("/auths/join")
                        .file(userPart)
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    @DisplayName("회원가입 실패 - 유효하지 않은 비밀번호")
    void join_invalidPassword() throws Exception {
        UserJoinRequestDTO user = UserJoinRequestDTO.builder()
                .email("invalidpw@example.com")
                .password("123")
                .nickname("정상닉네임")
                .build();

        MockMultipartFile userPart = new MockMultipartFile(
                "user", "", "application/json",
                objectMapper.writeValueAsBytes(user)
        );

        mockMvc.perform(multipart("/auths/join")
                        .file(userPart)
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("로그아웃 성공")
    void logout_success() throws Exception {
        // 로그인하여 토큰 획득
        UserLoginRequestDTO login = UserLoginRequestDTO.builder()
                .email(testEmail)
                .password(testPassword)
                .build();
        String token = mockMvc.perform(post("/auths/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String accessToken = objectMapper.readTree(token).get("accessToken").asText();

        mockMvc.perform(delete("/auths/logout")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk());
    }
}
