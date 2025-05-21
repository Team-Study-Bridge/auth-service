package com.example.authservice.service;

import com.example.authservice.dto.SendCodeResponseDTO;
import com.example.authservice.dto.VerifyCodeResponseDTO;
import com.example.authservice.mapper.EmailVerificationMapper;
import com.example.authservice.mapper.UserMapper;
import com.example.authservice.model.EmailVerification;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.javamail.JavaMailSender;

import java.sql.Timestamp;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@MockitoSettings(strictness = Strictness.LENIENT)
@ExtendWith(MockitoExtension.class)
class EmailVerificationServiceTest {

    @InjectMocks
    private EmailVerificationService emailVerificationService;

    @Mock private EmailVerificationMapper emailVerificationMapper;
    @Mock private JavaMailSender mailSender;
    @Mock private UserMapper userMapper;
    @Mock private MimeMessage mimeMessage;

    @BeforeEach
    void setup() throws MessagingException {
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
    }

    @Test
    void generateCode_shouldReturn6DigitString() {
        String code = emailVerificationService.generateCode();
        assertEquals(6, code.length());
    }

    @Test
    void sendVerificationCode_alreadyRegisteredEmail() {
        when(userMapper.findEmailByEmail("test@test.com")).thenReturn("test@test.com");

        ResponseEntity<SendCodeResponseDTO> response = emailVerificationService.sendVerificationCode("test@test.com");

        assertFalse(response.getBody().isSuccess());
        assertEquals("이미 가입된 이메일입니다.", response.getBody().getMessage());
    }

    @Test
    void sendVerificationCode_alreadyVerifiedEmail() {
        when(userMapper.findEmailByEmail("test@test.com")).thenReturn(null);
        when(emailVerificationMapper.findByEmail("test@test.com")).thenReturn(
                EmailVerification.builder().isVerified(true).build()
        );

        ResponseEntity<SendCodeResponseDTO> response = emailVerificationService.sendVerificationCode("test@test.com");

        assertTrue(response.getBody().isSuccess());
        assertEquals("이미 인증된 이메일입니다.", response.getBody().getMessage());
    }

    @Test
    void sendVerificationCode_existingButNotVerified_success() throws Exception {
        when(userMapper.findEmailByEmail("test@test.com")).thenReturn(null);
        when(emailVerificationMapper.findByEmail("test@test.com")).thenReturn(
                EmailVerification.builder().email("test@test.com").isVerified(false).build()
        );

        doNothing().when(emailVerificationMapper).updateEmailCode(any(), any(), anyBoolean(), any());
        doNothing().when(mailSender).send(any(MimeMessage.class));

        ResponseEntity<SendCodeResponseDTO> response = emailVerificationService.sendVerificationCode("test@test.com");

        assertTrue(response.getBody().isSuccess());
    }


    @Test
    void sendVerificationCode_newEmail_success() throws Exception {
        when(userMapper.findEmailByEmail("test@test.com")).thenReturn(null);
        when(emailVerificationMapper.findByEmail("test@test.com")).thenReturn(null);

        doNothing().when(emailVerificationMapper).insertEmailVerification(any());
        doNothing().when(mailSender).send(any(MimeMessage.class));

        ResponseEntity<SendCodeResponseDTO> response = emailVerificationService.sendVerificationCode("test@test.com");

        assertTrue(response.getBody().isSuccess());
    }


    @Test
    void verifyCode_notExist() {
        when(emailVerificationMapper.findByEmail("test@test.com")).thenReturn(null);

        ResponseEntity<VerifyCodeResponseDTO> response = emailVerificationService.verifyCode("test@test.com", "123456");

        assertFalse(response.getBody().isSuccess());
        assertEquals("인증 정보가 존재하지 않습니다. 처음부터 다시 시도해주세요.", response.getBody().getMessage());
    }

    @Test
    void verifyCode_alreadyVerified() {
        when(emailVerificationMapper.findByEmail("test@test.com")).thenReturn(
                EmailVerification.builder().isVerified(true).build()
        );

        ResponseEntity<VerifyCodeResponseDTO> response = emailVerificationService.verifyCode("test@test.com", "123456");

        assertTrue(response.getBody().isSuccess());
        assertEquals("이미 인증된 이메일입니다.", response.getBody().getMessage());
    }

    @Test
    void verifyCode_wrongCode() {
        when(emailVerificationMapper.findByEmail("test@test.com")).thenReturn(
                EmailVerification.builder().isVerified(false).code("999999").createdAt(Timestamp.valueOf(LocalDateTime.now())).build()
        );

        ResponseEntity<VerifyCodeResponseDTO> response = emailVerificationService.verifyCode("test@test.com", "123456");

        assertFalse(response.getBody().isSuccess());
        assertEquals("코드가 일치하지 않습니다.", response.getBody().getMessage());
    }

    @Test
    void verifyCode_expired() {
        when(emailVerificationMapper.findByEmail("test@test.com")).thenReturn(
                EmailVerification.builder()
                        .email("test@test.com")
                        .code("123456")
                        .isVerified(false)
                        .createdAt(Timestamp.valueOf(LocalDateTime.now().minusMinutes(10)))
                        .build()
        );

        doNothing().when(emailVerificationMapper).deleteByEmail("test@test.com");

        ResponseEntity<VerifyCodeResponseDTO> response = emailVerificationService.verifyCode("test@test.com", "123456");

        assertFalse(response.getBody().isSuccess());
        assertEquals("인증 시간이 초과되었습니다. 다시 시도해주세요.", response.getBody().getMessage());
    }

    @Test
    void verifyCode_success() {
        when(emailVerificationMapper.findByEmail("test@test.com")).thenReturn(
                EmailVerification.builder()
                        .email("test@test.com")
                        .code("123456")
                        .isVerified(false)
                        .createdAt(Timestamp.valueOf(LocalDateTime.now().minusMinutes(1)))
                        .build()
        );

        doNothing().when(emailVerificationMapper).updateEmailVerification(any());

        ResponseEntity<VerifyCodeResponseDTO> response = emailVerificationService.verifyCode("test@test.com", "123456");

        assertTrue(response.getBody().isSuccess());
        assertEquals("인증이 완료되었습니다.", response.getBody().getMessage());
    }

    @Test
    void isVerified_shouldReturnTrue() {
        when(emailVerificationMapper.findByEmail("test@test.com")).thenReturn(
                EmailVerification.builder().isVerified(true).build()
        );

        assertTrue(emailVerificationService.isVerified("test@test.com"));
    }

    @Test
    void isVerified_shouldReturnFalse() {
        when(emailVerificationMapper.findByEmail("test@test.com")).thenReturn(null);

        assertFalse(emailVerificationService.isVerified("test@test.com"));
    }

    @Test
    void deleteEmailVerification_shouldCallMapper() {
        when(emailVerificationMapper.findByEmail("test@test.com")).thenReturn(
                EmailVerification.builder().email("test@test.com").build()
        );

        doNothing().when(emailVerificationMapper).deleteByEmail("test@test.com");

        emailVerificationService.deleteEmailVerification("test@test.com");

        verify(emailVerificationMapper, times(1)).deleteByEmail("test@test.com");
    }
}
