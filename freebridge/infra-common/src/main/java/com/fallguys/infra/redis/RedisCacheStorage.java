package com.fallguys.infra.redis;

import com.fallguys.common.port.CacheStorage;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class RedisCacheStorage implements CacheStorage {

    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    public void set(String key, Object value, Duration timeout) {
        redisTemplate.opsForValue().set(key, value, timeout);
    }

    @Override
    public <T> Optional<T> get(String key, Class<T> type) {
        Object value = redisTemplate.opsForValue().get(key);
        if (value == null) return Optional.empty();

        if (!type.isInstance(value)) {
            return Optional.empty();
        }

        return Optional.of(type.cast(value));
    }

    @Override
    public void delete(String key) {
        redisTemplate.delete(key);
    }

    @Override
    public boolean hasKey(String key) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }
}