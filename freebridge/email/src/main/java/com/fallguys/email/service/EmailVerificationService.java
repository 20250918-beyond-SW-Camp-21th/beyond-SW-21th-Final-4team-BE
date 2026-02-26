package com.fallguys.email.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.fallguys.common.event.EmailVerifiedEvent;
import org.springframework.context.ApplicationEventPublisher;
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
    private final org.springframework.data.redis.core.script.RedisScript<Long> verifyFailScript;
    private final ApplicationEventPublisher applicationEventPublisher;

    private static final String REDIS_KEY_PREFIX = "email:verification:";
    private static final String REDIS_COUNT_PREFIX = "email:verification:count:";
    private static final long EXPIRATION_MINUTES = 5;
    private static final int CODE_LENGTH = 6;
    private static final long MAX_SENDS_PER_WINDOW = 5;

    /*
     * 인증코드 생성 → Redis 저장 → 이메일 발송 (발송 횟수 제한 포함)
     */
    public void sendVerificationCode(String email) {
        // 발송 횟수 제한 확인
        String countKey = REDIS_COUNT_PREFIX + email;
        Long sendCount = redisTemplate.opsForValue().increment(countKey);

        if (sendCount != null && sendCount == 1) {
            // 첫 발송 시 TTL 설정
            redisTemplate.expire(countKey, EXPIRATION_MINUTES, TimeUnit.MINUTES);
        }

        if (sendCount != null && sendCount > MAX_SENDS_PER_WINDOW) {
            log.warn("인증코드 발송 횟수 초과 - email: {}, count: {}", maskEmail(email), sendCount);
            throw new IllegalStateException("인증코드 발송 횟수를 초과했습니다. 잠시 후 다시 시도해주세요.");
        }

        String code = generateVerificationCode();

        // Redis에 인증코드 저장 (5분 TTL)
        String key = REDIS_KEY_PREFIX + email;
        String failKey = REDIS_FAIL_PREFIX + email;

        // 새 코드 발급 시 실패 횟수 초기화
        redisTemplate.delete(failKey);
        redisTemplate.opsForValue().set(key, code, EXPIRATION_MINUTES, TimeUnit.MINUTES);

        log.info("인증코드 생성 완료 - email: {}", maskEmail(email));

        // 이메일 발송
        emailService.sendVerificationEmail(email, code);
    }

    private static final String REDIS_FAIL_PREFIX = "email:verification:fails:";
    private static final long MAX_FAIL_ATTEMPTS = 5;

    /*
     * 인증코드 검증 (브루트포스 방어 포함)
     */
    public boolean verifyCode(String email, String code) {
        String key = REDIS_KEY_PREFIX + email;
        String failKey = REDIS_FAIL_PREFIX + email;

        // Lua 스크립트 실행 (원자적 검증)
        // KEYS: [failKey, codeKey], ARGV: [maxAttempts, providedCode, failTtlSeconds]
        Long result = redisTemplate.execute(
                verifyFailScript,
                java.util.Arrays.asList(failKey, key),
                String.valueOf(MAX_FAIL_ATTEMPTS),
                code,
                String.valueOf(TimeUnit.MINUTES.toSeconds(EXPIRATION_MINUTES)));

        if (result == null)
            return false;

        if (result == 1) { // SUCCESS
            log.info("이메일 인증 성공 - email: {}", maskEmail(email));
            // 회원가입 전 인증 상태 유지를 위해 Redis에 30분간 증표 저장
            String verifiedKey = "email:verified:" + email;
            redisTemplate.opsForValue().set(verifiedKey, "true", 30, TimeUnit.MINUTES);

            // 이벤트 발행 (기존 회원용)
            applicationEventPublisher.publishEvent(new EmailVerifiedEvent(email));
            return true;
        }

        if (result == -1) {
            log.warn("인증 시도 차단 (실패 횟수 초과) - email: {}", maskEmail(email));
        } else if (result == -2) {
            log.warn("인증코드 만료 또는 미존재 - email: {}", maskEmail(email));
        } else if (result == -3) {
            log.warn("최대 실패 횟수 도달 - 인증코드 무효화 - email: {}", maskEmail(email));
        } else {
            log.warn("인증코드 불일치 - email: {}", maskEmail(email));
        }

        return false;
    }

    /*
     * 6자리 숫자 인증코드 생성
     */
    private String generateVerificationCode() {
        SecureRandom random = new SecureRandom();
        int code = 100000 + random.nextInt(900000); // 100000 ~ 999999
        return String.valueOf(code);
    }

    /*
     * 이메일 마스킹 (예: test@gmail.com → t***t@gmail.com)
     */
    private String maskEmail(String email) {
        int atIndex = email.indexOf('@');
        if (atIndex <= 1)
            return "***" + email.substring(atIndex);
        String local = email.substring(0, atIndex);
        String domain = email.substring(atIndex);
        return local.charAt(0) + "***" + local.charAt(local.length() - 1) + domain;
    }
}
