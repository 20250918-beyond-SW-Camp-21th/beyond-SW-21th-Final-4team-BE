package com.fallguys.email.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "email_logs")
public class EmailLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "email_log_id")
    private Long id;

    // 회원가입 전 이메일 인증 등, 아직 user_id가 발급되지 않은 경우를 위해 nullable = true 허용
    @Column(name = "user_id", nullable = true)
    private Long userId;

    @Column(name = "receiver_email", nullable = false)
    private String receiverEmail;

    @Column(nullable = false)
    private String subject;

    @Lob
    @Column(nullable = false, columnDefinition = "LONGTEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EmailStatus status;

    @Column(name = "error_message")
    private String errorMessage;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @Builder
    public EmailLog(Long userId, String receiverEmail, String subject, String content, EmailStatus status,
            String errorMessage, LocalDateTime sentAt) {
        this.userId = userId;
        this.receiverEmail = maskEmail(receiverEmail);
        this.subject = subject;
        this.content = redactAuthCodes(content);
        this.status = status;
        this.errorMessage = errorMessage;
        this.sentAt = sentAt;
    }

    /**
     * 이메일 마스킹 (예: test@gmail.com → t***t@gmail.com)
     */
    private String maskEmail(String email) {
        if (email == null)
            return null;
        int atIndex = email.indexOf('@');
        if (atIndex <= 1)
            return "***" + (atIndex >= 0 ? email.substring(atIndex) : "");
        String local = email.substring(0, atIndex);
        String domain = email.substring(atIndex);
        return local.charAt(0) + "***" + local.charAt(local.length() - 1) + domain;
    }

    /**
     * 본문 내용의 인증코드(6자리 숫자 등) 보호
     */
    private String redactAuthCodes(String text) {
        if (text == null)
            return null;
        // 6자리 연속된 숫자를 [REDACTED] 로 치환
        return text.replaceAll("\\b\\d{6}\\b", "[REDACTED]");
    }

}
