package com.fallguys.chatting.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.concurrent.TimeUnit;

@Repository
@RequiredArgsConstructor
public class UnreadMessageRedisRepository {

    private final StringRedisTemplate redisTemplate;

    private static final String UNREAD_COUNT_KEY_PREFIX = "unread:";
    private static final long EXPIRE_DAYS = 30; // 30일 보관

    private String getKey(String roomId, String participantId) {
        return UNREAD_COUNT_KEY_PREFIX + roomId + ":" + participantId;
    }

    public void incrementUnreadCount(String roomId, String participantId) {
        String key = getKey(roomId, participantId);
        redisTemplate.opsForValue().increment(key);
        redisTemplate.expire(key, EXPIRE_DAYS, TimeUnit.DAYS);
    }

    public void resetUnreadCount(String roomId, String participantId) {
        String key = getKey(roomId, participantId);
        redisTemplate.opsForValue().set(key, "0");
        redisTemplate.expire(key, EXPIRE_DAYS, TimeUnit.DAYS);
    }

    public int getUnreadCount(String roomId, String participantId) {
        String countStr = redisTemplate.opsForValue().get(getKey(roomId, participantId));
        if (countStr == null) {
            return 0;
        }
        return Integer.parseInt(countStr);
    }
}
