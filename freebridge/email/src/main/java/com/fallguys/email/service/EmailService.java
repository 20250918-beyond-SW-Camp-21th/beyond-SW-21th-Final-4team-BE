package com.fallguys.email.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    /**
     * 인증코드 이메일 발송
     */
    public void sendVerificationEmail(String toEmail, String verificationCode) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("[FreeBridge] 이메일 인증 코드");
            helper.setText(buildEmailContent(verificationCode), true);

            mailSender.send(message);
            log.info("인증 이메일 발송 완료: {}", toEmail);
        } catch (MessagingException e) {
            log.error("이메일 발송 실패: {}", toEmail, e);
            throw new RuntimeException("이메일 발송에 실패했습니다.", e);
        }
    }

    /**
     * 이메일 HTML 본문 생성
     */
    private String buildEmailContent(String code) {
        return """
                <div style="max-width: 480px; margin: 0 auto; padding: 32px; font-family: 'Apple SD Gothic Neo', 'Noto Sans KR', sans-serif;">
                    <h2 style="color: #2563eb; margin-bottom: 24px;">FreeBridge 이메일 인증</h2>
                    <p style="font-size: 16px; color: #333; margin-bottom: 16px;">
                        아래 인증 코드를 입력해주세요.
                    </p>
                    <div style="background: #f1f5f9; border-radius: 12px; padding: 24px; text-align: center; margin-bottom: 24px;">
                        <span style="font-size: 32px; font-weight: 700; letter-spacing: 8px; color: #1e40af;">
                            %s
                        </span>
                    </div>
                    <p style="font-size: 14px; color: #666; margin-bottom: 8px;">
                        • 인증 코드는 <strong>5분간</strong> 유효합니다.
                    </p>
                    <p style="font-size: 14px; color: #666;">
                        • 본인이 요청하지 않은 경우 이 메일을 무시해주세요.
                    </p>
                    <hr style="border: none; border-top: 1px solid #e2e8f0; margin: 24px 0;">
                    <p style="font-size: 12px; color: #999;">
                        ⓒ FreeBridge. All rights reserved.
                    </p>
                </div>
                """
                .formatted(code);
    }
}
