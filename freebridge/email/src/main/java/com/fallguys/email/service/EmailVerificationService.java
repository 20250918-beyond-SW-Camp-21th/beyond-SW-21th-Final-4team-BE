package com.fallguys.email.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailVerificationService {

    private final EmailService emailService;
    private final StringRedisTemplate redisTemplate;

    private static final String REDIS_KEY_PREFIX = "email:verification:";
    private static final long EXPIRATION_MINUTES = 5;
    private static final int CODE_LENGTH = 6;

    /**
     * 인증코드 생성 → Redis 저장 → 이메일 발송
     */
    public void sendVerificationCode(String email) {
        String code = generateVerificationCode();

        // Redis에 인증코드 저장 (5분 TTL)
        String key = REDIS_KEY_PREFIX + email;
        redisTemplate.opsForValue().set(key, code, EXPIRATION_MINUTES, TimeUnit.MINUTES);

        log.info("인증코드 생성 - email: {}, code: {}", email, code);

        // 이메일 발송
        emailService.sendVerificationEmail(email, code);
    }

    /**
     * 인증코드 검증
     */
    public boolean verifyCode(String email, String code) {
        String key = REDIS_KEY_PREFIX + email;
        String storedCode = redisTemplate.opsForValue().get(key);

        if (storedCode == null) {
            log.warn("인증코드 만료 또는 미존재 - email: {}", email);
            return false;
        }

        if (storedCode.equals(code)) {
            // 인증 성공 → 사용된 코드 삭제
            redisTemplate.delete(key);
            log.info("이메일 인증 성공 - email: {}", email);
            return true;
        }

        log.warn("인증코드 불일치 - email: {}", email);
        return false;
    }

    /**
     * 6자리 숫자 인증코드 생성
     */
    private String generateVerificationCode() {
        SecureRandom random = new SecureRandom();
        int code = 100000 + random.nextInt(900000); // 100000 ~ 999999
        return String.valueOf(code);
    }
}
