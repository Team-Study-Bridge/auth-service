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
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
@Import(com.example.authservice.testsupport.TestConfiguration.class)
class UserControllerIntegrationTest {

    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:7.2-alpine")
            .withExposedPorts(6379)
            .waitingFor(Wait.forListeningPort());

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));

        // 추가 테스트 설정
        registry.add("spring.datasource.url", () -> "jdbc:h2:mem:testdb;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("logging.level.org.springframework.web.servlet.mvc.method.annotation", () -> "DEBUG");
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private final String testEmail = "testuser@example.com";
    private final String testPassword = "Password123!";
    private final String testNickname = "통합테스트유저";

    @BeforeEach
    void setUp() throws Exception {
        System.out.println("=== 테스트 setUp 시작 ===");
        try {
            // Redis 컨테이너가 준비되었는지 확인
            if (!redis.isRunning()) {
                throw new RuntimeException("Redis container is not running");
            }
            System.out.println("Redis container is running on port: " + redis.getMappedPort(6379));

            createTestUser();
        } catch (Exception e) {
            System.err.println("setUp 실패: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
        System.out.println("=== 테스트 setUp 완료 ===");
    }

    private void createTestUser() throws Exception {
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
                "profileImage", "test.jpg", "image/jpeg", "test image content".getBytes()
        );

        try {
            MvcResult result = mockMvc.perform(multipart("/auths/join")
                            .file(userPart)
                            .file(profileImage)
                            .contentType(MediaType.MULTIPART_FORM_DATA))
                    .andDo(print())
                    .andReturn();

            int status = result.getResponse().getStatus();
            String content = result.getResponse().getContentAsString();

            System.out.println("회원가입 응답 상태: " + status);
            System.out.println("회원가입 응답 내용: " + content);

            if (status >= 400 && status != 409) { // 409는 중복 가입으로 예상되는 상황
                throw new RuntimeException("회원가입 실패: " + content);
            }
        } catch (Exception e) {
            System.err.println("회원가입 과정에서 오류 발생: " + e.getMessage());
            // 중복 가입 오류가 아닌 경우에만 예외를 다시 던짐
            if (!e.getMessage().contains("409") && !e.getMessage().contains("Conflict")) {
                throw e;
            }
        }
    }


    @Test
    @DisplayName("간단한 상태 확인 테스트")
    void healthCheck() throws Exception {
        System.out.println("=== 헬스체크 테스트 시작 ===");

        // 가장 간단한 GET 요청으로 서버 상태 확인
        mockMvc.perform(get("/auths/users/by-type?type=ALL"))
                .andDo(print())
                .andExpect(status().isOk());

        System.out.println("=== 헬스체크 테스트 완료 ===");
    }

    @Test
    @DisplayName("로그인 성공")
    void login_success() throws Exception {
        System.out.println("=== 로그인 테스트 시작 ===");

        UserLoginRequestDTO login = UserLoginRequestDTO.builder()
                .email(testEmail)
                .password(testPassword)
                .build();

        mockMvc.perform(post("/auths/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.loggedIn").value(true));

        System.out.println("=== 로그인 테스트 완료 ===");
    }

    @Test
    @DisplayName("회원가입 - 새로운 사용자")
    void join_newUser() throws Exception {
        System.out.println("=== 새 사용자 회원가입 테스트 시작 ===");

        UserJoinRequestDTO user = UserJoinRequestDTO.builder()
                .email("newuser" + System.currentTimeMillis() + "@example.com") // 고유한 이메일
                .password(testPassword)
                .nickname("새사용자" + System.currentTimeMillis())
                .build();

        MockMultipartFile userPart = new MockMultipartFile(
                "user", "", "application/json",
                objectMapper.writeValueAsBytes(user)
        );

        MockMultipartFile profileImage = new MockMultipartFile(
                "profileImage", "test.jpg", "image/jpeg", "test image content".getBytes()
        );

        MvcResult result = mockMvc.perform(multipart("/auths/join")
                        .file(userPart)
                        .file(profileImage)
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andDo(print())
                .andReturn();

        int status = result.getResponse().getStatus();
        System.out.println("새 사용자 회원가입 응답 상태: " + status);
        System.out.println("새 사용자 회원가입 응답 내용: " + result.getResponse().getContentAsString());

        // 201 (Created) 또는 200 (OK) 둘 다 허용
        if (status != 200 && status != 201) {
            throw new AssertionError("Expected status 200 or 201 but was: " + status);
        }

        System.out.println("=== 새 사용자 회원가입 테스트 완료 ===");
    }

    @Test
    @DisplayName("유저 타입 조회 테스트")
    void getUsersByType_test() throws Exception {
        System.out.println("=== 유저 타입 조회 테스트 시작 ===");

        // ALL
        mockMvc.perform(get("/auths/users/by-type?type=ALL"))
                .andDo(print())
                .andExpect(status().isOk());

        // STUDENTS
        mockMvc.perform(get("/auths/users/by-type?type=STUDENTS"))
                .andDo(print())
                .andExpect(status().isOk());

        // INSTRUCTORS
        mockMvc.perform(get("/auths/users/by-type?type=INSTRUCTORS"))
                .andDo(print())
                .andExpect(status().isOk());

        System.out.println("=== 유저 타입 조회 테스트 완료 ===");
    }
}