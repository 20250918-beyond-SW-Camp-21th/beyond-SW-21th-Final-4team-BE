package com.fallguys.user.service;

import com.fallguys.common.security.TokenBlacklistService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedisTokenService implements TokenBlacklistService {

    private final StringRedisTemplate redisTemplate;

    private static final String REFRESH_TOKEN_PREFIX = "refresh_token:";
    private static final String BLACKLIST_PREFIX = "blacklist_token:";

    @Override
    public boolean isBlacklisted(String token) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(BLACKLIST_PREFIX + token));
    }

    public void addToBlacklist(String token, long remainingTimeMs) {
        if (remainingTimeMs > 0) {
            redisTemplate.opsForValue().set(BLACKLIST_PREFIX + token, "logout", remainingTimeMs, TimeUnit.MILLISECONDS);
            log.info("Token added to blacklist. TTL: {} ms", remainingTimeMs);
        }
    }

    public void saveRefreshToken(Long userId, String refreshToken, long durationMs) {
        redisTemplate.opsForValue().set(REFRESH_TOKEN_PREFIX + userId, refreshToken, durationMs, TimeUnit.MILLISECONDS);
        log.info("Refresh token saved for user: {}", userId);
    }

    public void deleteRefreshToken(Long userId) {
        redisTemplate.delete(REFRESH_TOKEN_PREFIX + userId);
        log.info("Refresh token deleted for user: {}", userId);
    }

    public String getRefreshToken(Long userId) {
        return redisTemplate.opsForValue().get(REFRESH_TOKEN_PREFIX + userId);
    }
}
