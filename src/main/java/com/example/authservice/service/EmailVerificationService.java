package com.example.authservice.service;

import com.example.authservice.mapper.EmailVerificationMapper;
import com.example.authservice.model.EmailVerification;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class EmailVerificationService {

    private final EmailVerificationMapper emailVerificationMapper;
    private final JavaMailSender mailSender;

    public String generateCode() {
        int code = 100000 + new Random().nextInt(900000);  // 6자리 랜덤 코드 생성
        return String.valueOf(code);
    }

    public void sendVerificationCode(String email) {
        String code = generateCode();

        EmailVerification verification = EmailVerification.builder()
                .email(email)
                .code(code)
                .isVerified(false)
                .createdAt(new Timestamp(System.currentTimeMillis()))
                .build();

        emailVerificationMapper.insertEmailVerification(verification);

        // 이메일 보내기
        sendEmail(email, code);
    }

    public void sendEmail(String toEmail, String code) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);

            helper.setTo(toEmail);
            helper.setSubject("이메일 인증 코드");
            helper.setText("아래 인증 코드를 입력하세요: \n\n" + code);

            mailSender.send(message);
        } catch (MessagingException e) {
            e.printStackTrace();
        }
    }

    public boolean verifyCode(String email, String code) {
        EmailVerification verification = emailVerificationMapper.findByEmailAndCode(email, code);

        if (verification != null && !verification.isVerified()) {
            verification.setVerified(true);  // 인증 완료 처리

            emailVerificationMapper.updateEmailVerification(verification);
            return true;
        }
        return false;
    }

    public boolean isVerified(String email) {
        EmailVerification verification = emailVerificationMapper.findByEmail(email);

        if (verification != null) {
            return verification.isVerified();  // null 체크 후 인증 여부 확인
        }

        return false;
    }
}
