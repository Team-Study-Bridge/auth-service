package com.example.authservice.service;

import com.example.authservice.dto.SendCodeResponseDTO;
import com.example.authservice.dto.VerifyCodeResponseDTO;
import com.example.authservice.mapper.EmailVerificationMapper;
import com.example.authservice.mapper.UserMapper;
import com.example.authservice.model.EmailVerification;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Random;


@Service
@RequiredArgsConstructor
public class EmailVerificationService {

    private final EmailVerificationMapper emailVerificationMapper;
    private final JavaMailSender mailSender;
    private final UserMapper userMapper;

    public String generateCode() {
        int code = 100000 + new Random().nextInt(900000);  // 6자리 랜덤 코드 생성
        return String.valueOf(code);
    }

    public SendCodeResponseDTO sendVerificationCode(String email) {

        String emailByEmail = userMapper.findEmailByEmail(email);
        if (emailByEmail != null) {
            return SendCodeResponseDTO.builder()
                    .success(false)
                    .message("이미 가입된 이메일입니다.")
                    .build();
        }

        EmailVerification byEmail = emailVerificationMapper.findByEmail(email);

        if (byEmail != null && byEmail.isVerified()) {
            return SendCodeResponseDTO.builder()
                    .success(true)
                    .message("이미 인증된 이메일입니다.")
                    .build();
        }

        String code = generateCode();
        LocalDateTime now = LocalDateTime.now();

        if (byEmail != null && !byEmail.isVerified()) {
            EmailVerification verification = EmailVerification.builder()
                    .email(email)
                    .code(code)
                    .isVerified(false)
                    .createdAt(Timestamp.valueOf(now))
                    .build();
            try {
                emailVerificationMapper.updateEmailCode(verification.getEmail(), verification.getCode(), verification.isVerified(), verification.getCreatedAt());
                // 이메일 보내기
                sendEmail(email, code);

                return  SendCodeResponseDTO.builder()
                        .success(true)
                        .message("이메일에서 코드를 확인해주세요.")
                        .build();
            }catch (Exception e){
                return SendCodeResponseDTO.builder()
                        .success(false)
                        .message("이메일 재발송중 오류가 발생하였습니다")
                        .build();
            }
        }
        EmailVerification verification = EmailVerification.builder()
                .email(email)
                .code(code)
                .isVerified(false)
                .createdAt(Timestamp.valueOf(now))
                .build();
        try {
            emailVerificationMapper.insertEmailVerification(verification);
            // 이메일 보내기
            sendEmail(email, code);

            return SendCodeResponseDTO.builder()
                    .success(true)
                    .message("이메일에서 코드를 확인해주세요.")
                    .build();
        }catch (Exception e){
            return SendCodeResponseDTO.builder()
                    .success(false)
                    .message("이메일 발송중 오류가 발생하였습니다")
                    .build();
        }
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


    public VerifyCodeResponseDTO verifyCode(String email, String code) {
        EmailVerification verification = emailVerificationMapper.findByEmail(email);

        if (verification == null) {
            return VerifyCodeResponseDTO.builder()
                    .success(false)
                    .message("인증 정보가 존재하지 않습니다. 처음부터 다시 시도해주세요.")
                    .build();
        }

        if (verification.isVerified()) {
            return VerifyCodeResponseDTO.builder()
                    .success(true)
                    .message("이미 인증된 이메일입니다.")
                    .build();
        }

        if (!verification.getCode().equals(code)) {  // String 비교는 .equals() 사용!
            return VerifyCodeResponseDTO.builder()
                    .success(false)
                    .message("코드가 일치하지 않습니다.")
                    .build();
        }

        LocalDateTime createdAt = verification.getCreatedAt().toLocalDateTime();
        LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Seoul"));

        if (createdAt.plusMinutes(5).isBefore(now)) {
            deleteEmailVerification(email);  // 만료된 인증 정보 삭제

            return VerifyCodeResponseDTO.builder()
                    .success(false)
                    .message("인증 시간이 초과되었습니다. 다시 시도해주세요.")
                    .build();
        }

        verification.setVerified(true);
        emailVerificationMapper.updateEmailVerification(verification);

        return VerifyCodeResponseDTO.builder()
                .success(true)
                .message("인증이 완료되었습니다.")
                .build();
    }


    public boolean isVerified(String email) {
        EmailVerification verification = emailVerificationMapper.findByEmail(email);

        if (verification != null) {
            return verification.isVerified();  // null 체크 후 인증 여부 확인
        }

        return false;
    }

    public void deleteEmailVerification(String email) {
        EmailVerification verification = emailVerificationMapper.findByEmail(email);

        if (verification != null) {
            emailVerificationMapper.deleteByEmail(email);
        }
    }
}


