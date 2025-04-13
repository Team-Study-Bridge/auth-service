package com.example.authservice.service;

import com.example.authservice.dto.SendCodeResponseDTO;
import com.example.authservice.dto.VerifyCodeResponseDTO;
import com.example.authservice.mapper.EmailVerificationMapper;
import com.example.authservice.mapper.UserMapper;
import com.example.authservice.model.EmailVerification;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.javamail.JavaMailSender;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Objects;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class EmailVerificationServiceTest {

    @InjectMocks
    private EmailVerificationService emailVerificationService;

    @Mock
    private EmailVerificationMapper emailVerificationMapper;

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private UserMapper userMapper;

    // 이메일 중복 가입된 경우
    @Test
    void 이미_가입된_이메일이면_실패() {
        String email = "test@example.com";
        given(userMapper.findEmailByEmail(email)).willReturn(email);

        ResponseEntity<SendCodeResponseDTO> response = emailVerificationService.sendVerificationCode(email);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().isSuccess()).isFalse();
        assertThat(response.getBody().getMessage()).contains("이미 가입된 이메일");
    }

    // 이미 인증된 이메일
    @Test
    void 이미_인증된_이메일이면_성공_반환() {
        String email = "test@example.com";
        given(userMapper.findEmailByEmail(email)).willReturn(null);
        given(emailVerificationMapper.findByEmail(email))
                .willReturn(EmailVerification.builder().email(email).isVerified(true).build());

        ResponseEntity<SendCodeResponseDTO> response = emailVerificationService.sendVerificationCode(email);

        assertThat(response.getBody().isSuccess()).isTrue();
        assertThat(response.getBody().getMessage()).contains("이미 인증된 이메일");
    }

    // 기존 인증 있음 + 미인증 → 코드 업데이트 후 전송 성공
    @Test
    void 기존_인증있고_미인증이면_코드업데이트후_이메일전송() {
        String email = "test@example.com";
        MimeMessage mimeMessage = Mockito.mock(MimeMessage.class);
        given(userMapper.findEmailByEmail(email)).willReturn(null);
        given(emailVerificationMapper.findByEmail(email))
                .willReturn(EmailVerification.builder().email(email).isVerified(false).build());
        given(mailSender.createMimeMessage()).willReturn(mimeMessage);

        ResponseEntity<SendCodeResponseDTO> response = emailVerificationService.sendVerificationCode(email);

        assertThat(response.getBody().isSuccess()).isTrue();
    }

    // 새 인증 정보 삽입 성공
    @Test
    void 신규_이메일이면_인증정보_저장후_이메일전송() {
        String email = "new@example.com";
        MimeMessage mimeMessage = Mockito.mock(MimeMessage.class);
        given(userMapper.findEmailByEmail(email)).willReturn(null);
        given(emailVerificationMapper.findByEmail(email)).willReturn(null);
        given(mailSender.createMimeMessage()).willReturn(mimeMessage);

        ResponseEntity<SendCodeResponseDTO> response = emailVerificationService.sendVerificationCode(email);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().isSuccess()).isTrue();
    }

    // verifyCode - 코드 일치 실패
    @Test
    void 인증코드_불일치시_실패() {
        String email = "test@example.com";
        EmailVerification verification = EmailVerification.builder()
                .email(email)
                .code("123456")
                .isVerified(false)
                .createdAt(Timestamp.valueOf(LocalDateTime.now()))
                .build();

        given(emailVerificationMapper.findByEmail(email)).willReturn(verification);

        ResponseEntity<VerifyCodeResponseDTO> response = emailVerificationService.verifyCode(email, "000000");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getMessage()).contains("코드가 일치하지 않습니다");
    }

    // verifyCode - 시간 초과 (만료)
    @Test
    void 인증시간_초과시_삭제후_실패() {
        String email = "test@example.com";
        EmailVerification verification = EmailVerification.builder()
                .email(email)
                .code("123456")
                .isVerified(false)
                .createdAt(Timestamp.valueOf(LocalDateTime.now().minusMinutes(6)))
                .build();

        given(emailVerificationMapper.findByEmail(email)).willReturn(verification);

        ResponseEntity<VerifyCodeResponseDTO> response = emailVerificationService.verifyCode(email, "123456");

        assertThat(response.getBody().getMessage()).contains("인증 시간이 초과되었습니다");
    }
    @Test
    void 이메일인증확인_true_반환() {
        String email = "test@example.com";
        given(emailVerificationMapper.findByEmail(email))
                .willReturn(EmailVerification.builder().email(email).isVerified(true).build());

        boolean result = emailVerificationService.isVerified(email);
        assertThat(result).isTrue();
    }

    @Test
    void 이메일인증확인_false_반환() {
        String email = "test@example.com";
        given(emailVerificationMapper.findByEmail(email))
                .willReturn(EmailVerification.builder().email(email).isVerified(false).build());

        boolean result = emailVerificationService.isVerified(email);
        assertThat(result).isFalse();
    }

    @Test
    void verifyCode_인증정보없으면_실패() {
        String email = "test@example.com";
        given(emailVerificationMapper.findByEmail(email)).willReturn(null);

        ResponseEntity<VerifyCodeResponseDTO> response =
                emailVerificationService.verifyCode(email, "123456");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(Objects.requireNonNull(response.getBody()).getSuccess()).isFalse();
        assertThat(response.getBody().getMessage()).contains("인증 정보가 존재하지 않습니다");
    }

    @Test
    void verifyCode_이미인증된이메일이면_성공반환() {
        String email = "test@example.com";
        EmailVerification verification = EmailVerification.builder()
                .email(email)
                .isVerified(true)
                .build();

        given(emailVerificationMapper.findByEmail(email)).willReturn(verification);

        ResponseEntity<VerifyCodeResponseDTO> response =
                emailVerificationService.verifyCode(email, "123456");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(Objects.requireNonNull(response.getBody()).getSuccess()).isTrue();
        assertThat(response.getBody().getMessage()).contains("이미 인증된 이메일입니다");
    }



    @Test
    void 이메일_재발송_중_예외발생시_실패() {
        String email = "test@example.com";
        EmailVerification verification = EmailVerification.builder()
                .email(email)
                .isVerified(false)
                .build();

        MimeMessage mimeMessage = Mockito.mock(MimeMessage.class);

        given(userMapper.findEmailByEmail(email)).willReturn(null);
        given(emailVerificationMapper.findByEmail(email)).willReturn(verification);
        given(mailSender.createMimeMessage()).willReturn(mimeMessage);
        Mockito.doThrow(new RuntimeException("메일 전송 실패")).when(mailSender).send(Mockito.any(MimeMessage.class));

        ResponseEntity<SendCodeResponseDTO> response = emailVerificationService.sendVerificationCode(email);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().isSuccess()).isFalse();
        assertThat(response.getBody().getMessage()).contains("이메일 재발송중 오류");
    }

    @Test
    void 이메일인증확인_정보없을시_false() {
        String email = "test@example.com";
        given(emailVerificationMapper.findByEmail(email)).willReturn(null);

        boolean result = emailVerificationService.isVerified(email);
        assertThat(result).isFalse();
    }


    // verifyCode - 성공
    @Test
    void 인증코드_정상시_성공() {
        String email = "test@example.com";
        EmailVerification verification = EmailVerification.builder()
                .email(email)
                .code("123456")
                .isVerified(false)
                .createdAt(Timestamp.valueOf(LocalDateTime.now()))
                .build();

        given(emailVerificationMapper.findByEmail(email)).willReturn(verification);

        ResponseEntity<VerifyCodeResponseDTO> response = emailVerificationService.verifyCode(email, "123456");

        assertThat(Objects.requireNonNull(response.getBody()).getSuccess()).isTrue();
        assertThat(response.getBody().getMessage()).contains("인증이 완료");
    }

    @Test
    void 기존인증정보없을때_새로생성됨() {
        // given
        String email = "newuser@example.com";
        MimeMessage mimeMessage = Mockito.mock(MimeMessage.class);

        given(userMapper.findEmailByEmail(email)).willReturn(null); // 가입되지 않은 이메일
        given(emailVerificationMapper.findByEmail(email)).willReturn(null); // 인증 정보 없음
        given(mailSender.createMimeMessage()).willReturn(mimeMessage);

        // when
        ResponseEntity<SendCodeResponseDTO> response =
                emailVerificationService.sendVerificationCode(email);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().isSuccess()).isTrue();

        // 인증 정보 저장 시도 검증
        Mockito.verify(emailVerificationMapper).insertEmailVerification(Mockito.any(EmailVerification.class));
    }



    @Test
    void deleteEmailVerification_정보없으면_삭제안함() {
        String email = "test@example.com";
        given(emailVerificationMapper.findByEmail(email)).willReturn(null);

        // 실행
        emailVerificationService.deleteEmailVerification(email);

        // 삭제 호출이 없어야 함
        Mockito.verify(emailVerificationMapper, Mockito.never()).deleteByEmail(email);
    }
}

