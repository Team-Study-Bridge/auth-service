package com.example.authservice.service;

import com.example.authservice.dto.SendCodeResponseDTO;
import com.example.authservice.dto.VerifyCodeResponseDTO;
import com.example.authservice.mapper.EmailVerificationMapper;
import com.example.authservice.mapper.UserMapper;
import com.example.authservice.model.EmailVerification;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Random;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailVerificationService {

    private final EmailVerificationMapper emailVerificationMapper;
    private final JavaMailSender mailSender;
    private final UserMapper userMapper;

    // 6자리의 랜덤 인증 코드 생성
    public String generateCode() {
        int code = 100000 + new Random().nextInt(900000);
        return String.valueOf(code);
    }

    // 이메일 인증 코드를 전송하는 메서드 (ResponseEntity와 DTO 빌더 사용)
    public ResponseEntity<SendCodeResponseDTO> sendVerificationCode(String email) {
        // 이미 가입된 이메일인지 확인
        String emailByEmail = userMapper.findEmailByEmail(email);
        if (emailByEmail != null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(SendCodeResponseDTO.builder()
                            .success(false)
                            .message("이미 가입된 이메일입니다.")
                            .build());
        }

        EmailVerification existingVerification = emailVerificationMapper.findByEmail(email);
        // 이미 인증된 경우
        if (existingVerification != null && existingVerification.isVerified()) {
            return ResponseEntity.ok(
                    SendCodeResponseDTO.builder()
                            .success(true)
                            .message("이미 인증된 이메일입니다.")
                            .build()
            );
        }

        String code = generateCode();
        LocalDateTime now = LocalDateTime.now();

        // 기존 이메일 인증 정보가 있으나 아직 인증되지 않은 경우 - 코드 업데이트 후 이메일 발송
        if (existingVerification != null && !existingVerification.isVerified()) {
            EmailVerification verification = EmailVerification.builder()
                    .email(email)
                    .code(code)
                    .isVerified(false)
                    .createdAt(Timestamp.valueOf(now))
                    .build();
            try {
                emailVerificationMapper.updateEmailCode(
                        verification.getEmail(),
                        verification.getCode(),
                        verification.isVerified(),
                        verification.getCreatedAt()
                );
                sendEmail(email, code);
                return ResponseEntity.ok(
                        SendCodeResponseDTO.builder()
                                .success(true)
                                .message("이메일에서 코드를 확인해주세요.")
                                .build()
                );
            } catch (Exception e) {
                log.error("이메일 재발송중 오류 발생: {}", e.getMessage());
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(SendCodeResponseDTO.builder()
                                .success(false)
                                .message("이메일 재발송중 오류가 발생하였습니다")
                                .build());
            }
        }

        // 새로운 이메일 인증 정보 생성 후 저장
        EmailVerification verification = EmailVerification.builder()
                .email(email)
                .code(code)
                .isVerified(false)
                .createdAt(Timestamp.valueOf(now))
                .build();
        try {
            emailVerificationMapper.insertEmailVerification(verification);
            sendEmail(email, code);
            System.out.println("이구간 지나감");
            return ResponseEntity.ok(
                    SendCodeResponseDTO.builder()
                            .success(true)
                            .message("이메일에서 코드를 확인해주세요.")
                            .build()
            );
        } catch (Exception e) {
            log.error("이메일 발송중 오류 발생: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(SendCodeResponseDTO.builder()
                            .success(false)
                            .message("이메일 발송중 오류가 발생하였습니다")
                            .build());
        }
    }

    // 실제 이메일 전송 역할 (내부 로직 처리)
    public void sendEmail(String toEmail, String code) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            helper.setTo(toEmail);
            helper.setSubject("이메일 인증 코드");
            helper.setText("아래 인증 코드를 입력하세요: \n\n" + code);
            mailSender.send(message);
        } catch (MessagingException e) {
            log.error("이메일 전송 중 오류 발생: {}", e.getMessage());
            throw new RuntimeException("이메일 전송 실패", e);
        }
    }

    // 이메일 코드 검증 후 결과를 반환하는 메서드
    public ResponseEntity<VerifyCodeResponseDTO> verifyCode(String email, String code) {
        EmailVerification verification = emailVerificationMapper.findByEmail(email);
        if (verification == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(VerifyCodeResponseDTO.builder()
                            .success(false)
                            .message("인증 정보가 존재하지 않습니다. 처음부터 다시 시도해주세요.")
                            .build());
        }

        if (verification.isVerified()) {
            return ResponseEntity.ok(
                    VerifyCodeResponseDTO.builder()
                            .success(true)
                            .message("이미 인증된 이메일입니다.")
                            .build());
        }

        if (!verification.getCode().equals(code)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(VerifyCodeResponseDTO.builder()
                            .success(false)
                            .message("코드가 일치하지 않습니다.")
                            .build());
        }

        LocalDateTime createdAt = verification.getCreatedAt().toLocalDateTime();
        LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Seoul"));

        if (createdAt.plusMinutes(5).isBefore(now)) {
            deleteEmailVerification(email);  // 만료된 인증 정보 삭제
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(VerifyCodeResponseDTO.builder()
                            .success(false)
                            .message("인증 시간이 초과되었습니다. 다시 시도해주세요.")
                            .build());
        }

        verification.setVerified(true);
        emailVerificationMapper.updateEmailVerification(verification);
        return ResponseEntity.ok(
                VerifyCodeResponseDTO.builder()
                        .success(true)
                        .message("인증이 완료되었습니다.")
                        .build());
    }

    // 이메일의 인증 여부 확인 (내부 로직)
    public boolean isVerified(String email) {
        EmailVerification verification = emailVerificationMapper.findByEmail(email);
        return verification != null && verification.isVerified();
    }

//     특정 이메일에 대한 인증정보 삭제 (내부 로직)
    public void deleteEmailVerification(String email) {
        EmailVerification verification = emailVerificationMapper.findByEmail(email);
        if (verification != null) {
            emailVerificationMapper.deleteByEmail(email);
        }
    }
}
