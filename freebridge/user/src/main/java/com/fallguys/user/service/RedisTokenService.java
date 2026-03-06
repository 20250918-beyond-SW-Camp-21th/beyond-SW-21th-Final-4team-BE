package com.fallguys.user.service;

import com.fallguys.common.security.TokenBlacklistService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class RedisTokenService implements TokenBlacklistService {

    private final StringRedisTemplate redisTemplate;
    private final SecretKeySpec refreshTokenHmacKey;

    private static final String REFRESH_TOKEN_PREFIX = "refresh_token:";
    private static final String BLACKLIST_PREFIX = "blacklist_token:";
    private static final String REVOKED_REFRESH_JTI_PREFIX = "revoked_refresh_jti:";
    private static final DefaultRedisScript<Long> REFRESH_TOKEN_CAS_SCRIPT = buildRefreshTokenCasScript();

    public RedisTokenService(
            StringRedisTemplate redisTemplate,
            @Value("${security.refresh-token.hmac-secret:}") String refreshTokenHmacSecret
    ) {
        this.redisTemplate = redisTemplate;
        if (refreshTokenHmacSecret == null || refreshTokenHmacSecret.isBlank()) {
            throw new IllegalStateException("Missing required config: security.refresh-token.hmac-secret (env: REFRESH_TOKEN_HMAC_SECRET)");
        }
        this.refreshTokenHmacKey = new SecretKeySpec(refreshTokenHmacSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }

    @Override
    public boolean isBlacklisted(String token) {
        // Backward-compatible: accept legacy raw-token keys and new hashed keys.
        if (Boolean.TRUE.equals(redisTemplate.hasKey(BLACKLIST_PREFIX + token))) {
            return true;
        }
        return Boolean.TRUE.equals(redisTemplate.hasKey(BLACKLIST_PREFIX + hmacSha256Base64Url(token)));
    }

    public void addToBlacklist(String token, long remainingTimeMs) {
        if (remainingTimeMs > 0) {
            redisTemplate.opsForValue().set(BLACKLIST_PREFIX + hmacSha256Base64Url(token), "logout", remainingTimeMs, TimeUnit.MILLISECONDS);
            log.info("Token added to blacklist. TTL: {} ms", remainingTimeMs);
        }
    }

    public void saveRefreshToken(Long userId, String refreshToken, String jti, long durationMs) {
        String record = buildRefreshTokenRecord(refreshToken, jti);
        redisTemplate.opsForValue().set(REFRESH_TOKEN_PREFIX + userId, record, durationMs, TimeUnit.MILLISECONDS);
        log.info("Refresh token saved for user: {}", userId);
    }

    public void deleteRefreshToken(Long userId) {
        String key = REFRESH_TOKEN_PREFIX + userId;
        String record = redisTemplate.opsForValue().get(key);
        if (record != null) {
            String jti = extractJtiFromRecord(record);
            Long ttlMs = redisTemplate.getExpire(key, TimeUnit.MILLISECONDS);
            if (jti != null && ttlMs != null && ttlMs > 0) {
                revokeRefreshTokenJti(jti, ttlMs);
            }
        }

        redisTemplate.delete(key);
        log.info("Refresh token deleted for user: {}", userId);
    }

    public String getRefreshTokenRecord(Long userId) {
        return redisTemplate.opsForValue().get(REFRESH_TOKEN_PREFIX + userId);
    }

    public String getRefreshTokenJti(Long userId) {
        String record = getRefreshTokenRecord(userId);
        return extractJtiFromRecord(record);
    }

    public boolean isRefreshTokenJtiRevoked(String jti) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(REVOKED_REFRESH_JTI_PREFIX + jti));
    }

    public void revokeRefreshTokenJti(String jti, long remainingTimeMs) {
        if (remainingTimeMs > 0) {
            redisTemplate.opsForValue().set(REVOKED_REFRESH_JTI_PREFIX + jti, "revoked", remainingTimeMs, TimeUnit.MILLISECONDS);
        }
    }

    public boolean compareAndSetRefreshToken(
            Long userId,
            String expectedRefreshToken,
            String expectedJti,
            String newRefreshToken,
            String newJti,
            long durationMs
    ) {
        String key = REFRESH_TOKEN_PREFIX + userId;
        String expectedRecord = buildRefreshTokenRecord(expectedRefreshToken, expectedJti);
        String newRecord = buildRefreshTokenRecord(newRefreshToken, newJti);
        Long result = redisTemplate.execute(
                REFRESH_TOKEN_CAS_SCRIPT,
                Collections.singletonList(key),
                expectedRecord,
                newRecord,
                String.valueOf(durationMs)
        );
        boolean success = result != null && result == 1L;
        if (success) {
            log.info("Refresh token rotated for user: {}", userId);
        }
        return success;
    }

    public boolean matchesStoredRefreshToken(Long userId, String refreshToken, String jti) {
        String record = getRefreshTokenRecord(userId);
        if (record == null) {
            return false;
        }

        int idx = record.lastIndexOf(':');
        if (idx <= 0 || idx == record.length() - 1) {
            return false;
        }

        String storedDigest = record.substring(0, idx);
        String storedJti = record.substring(idx + 1);
        if (!MessageDigest.isEqual(storedJti.getBytes(StandardCharsets.UTF_8), jti.getBytes(StandardCharsets.UTF_8))) {
            return false;
        }

        String expectedDigest = hmacSha256Base64Url(refreshToken);
        return constantTimeEquals(storedDigest, expectedDigest);
    }

    private static DefaultRedisScript<Long> buildRefreshTokenCasScript() {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setResultType(Long.class);
        script.setScriptText(
                "if redis.call('get', KEYS[1]) == ARGV[1] then " +
                        "redis.call('psetex', KEYS[1], ARGV[3], ARGV[2]); " +
                        "return 1; " +
                        "else return 0; end"
        );
        return script;
    }

    private String buildRefreshTokenRecord(String refreshToken, String jti) {
        if (refreshToken == null || refreshToken.isBlank() || jti == null || jti.isBlank()) {
            throw new IllegalArgumentException("refreshToken and jti must be non-empty");
        }
        return hmacSha256Base64Url(refreshToken) + ":" + jti;
    }

    private static String extractJtiFromRecord(String record) {
        if (record == null) {
            return null;
        }
        int idx = record.lastIndexOf(':');
        if (idx <= 0 || idx == record.length() - 1) {
            return null;
        }
        return record.substring(idx + 1);
    }

    private String hmacSha256Base64Url(String value) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(refreshTokenHmacKey);
            byte[] digest = mac.doFinal(value.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("HmacSHA256 not available", e);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to compute HMAC", e);
        }
    }

    private static boolean constantTimeEquals(String a, String b) {
        return MessageDigest.isEqual(
                a.getBytes(StandardCharsets.UTF_8),
                b.getBytes(StandardCharsets.UTF_8)
        );
    }
}
