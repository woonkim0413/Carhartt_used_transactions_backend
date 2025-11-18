package com.C_platform.Member_woonkim.infrastructure.mail;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.C_platform.Member_woonkim.exception.EmailException;
import com.C_platform.Member_woonkim.exception.EmailErrorCode;

/**
 * 이메일 발송 서비스
 * Gmail SMTP를 통해 인증 코드를 전송합니다.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private static final String FROM_EMAIL = "gamegemos588@gmail.com";
    private static final String SUBJECT = "Carhartt 이메일 인증 코드";

    /**
     * 이메일로 인증 코드 전송
     * @param toEmail 수신 이메일
     * @param code 6자리 난수 코드
     * @throws EmailException 메일 전송 실패 시
     */
    public void sendVerificationCodeEmail(
            String toEmail, String code, JavaMailSender javaMailSender) throws EmailException {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(FROM_EMAIL);
            message.setTo(toEmail);
            message.setSubject(SUBJECT);
            message.setText(buildEmailContent(code));

            javaMailSender.send(message);
            log.info("Verification code sent to: {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send verification code to {}", toEmail, e);
            throw new EmailException(EmailErrorCode.E001);
        }
    }

    /**
     * 이메일 본문 구성
     */
    private String buildEmailContent(String code) {
        return String.format(
            "안녕하세요,\n\n" +
            "Carhartt 회원가입을 위한 인증 코드입니다.\n\n" +
            "인증 코드: %s\n\n" +
            "이 코드는 10분 동안 유효합니다.\n" +
            "본인이 요청하지 않았다면 이 이메일을 무시해주세요.\n\n" +
            "감사합니다.",
            code
        );
    }
}
