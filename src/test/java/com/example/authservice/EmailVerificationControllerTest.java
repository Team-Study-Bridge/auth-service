package com.example.authservice;

import com.example.authservice.dto.EmailRequestDTO;
import com.example.authservice.mapper.EmailVerificationMapper;
import com.example.authservice.model.EmailVerification;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.context.WebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
public class EmailVerificationControllerTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private EmailVerificationMapper emailVerificationMapper;

    private MockMvc mockMvc;

    @Test
    void testEmailVerificationFlow() throws Exception {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build();

        String email = "test1@example.com";

        // Step 1: 이메일 인증 코드 전송 요청
        EmailRequestDTO requestDTO = new EmailRequestDTO();
        requestDTO.setEmail(email);

        MvcResult sendCodeResult = mockMvc.perform(post("/api/email/send-code")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\"}"))
                .andExpect(status().isOk())
                .andReturn();

        String sendCodeResponse = sendCodeResult.getResponse().getContentAsString();
        assertThat(sendCodeResponse).contains("Verification code sent successfully to " + email);

        // Step 2: DB에 저장된 인증 코드 확인
        EmailVerification verification = emailVerificationMapper.findByEmail(email);
        assertThat(verification).isNotNull();

        String code = verification.getCode();

        // Step 3: 인증 코드 확인 요청
        MvcResult verifyCodeResult = mockMvc.perform(post("/api/email/verify-code")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\", \"code\":\"" + code + "\"}"))
                .andExpect(status().isOk())
                .andReturn();

        String verifyResponse = verifyCodeResult.getResponse().getContentAsString();
        assertThat(verifyResponse).isEqualTo("Email verified successfully.");

        // Step 4: 인증 완료 여부 확인
        EmailVerification updatedVerification = emailVerificationMapper.findByEmail(email);
        assertThat(updatedVerification.isVerified()).isTrue();
    }
}
