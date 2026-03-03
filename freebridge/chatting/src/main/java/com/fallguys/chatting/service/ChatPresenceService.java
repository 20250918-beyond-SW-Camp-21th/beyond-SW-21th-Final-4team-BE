package com.fallguys.chatting.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Slf4j
@RequiredArgsConstructor
@Service
public class ChatPresenceService {

    private final StringRedisTemplate redisTemplate;

    private static final String PRESENCE_PREFIX = "presence:";
    private static final String PRESENCE_LAST_SEEN_PREFIX = "presence:lastSeen:";
    // lastSeen TTL을 두어 서버 크래시 등 비정상 종료 시에도 좀비 상태가 남지 않도록 함
    private static final long PRESENCE_TIMEOUT_HOURS = 1;

    /**
     * 유저가 웹소켓에 연결되면 ONLINE 상태로 기록
     */
    public void connectUser(String userId) {
        String statusKey = PRESENCE_PREFIX + userId;
        String lastSeenKey = PRESENCE_LAST_SEEN_PREFIX + userId;
        // ONLINE 상태는 유지하되, lastSeen 키로 좀비 상태를 방지
        redisTemplate.opsForValue().set(statusKey, "ONLINE", PRESENCE_TIMEOUT_HOURS, TimeUnit.HOURS);
        redisTemplate.opsForValue().set(lastSeenKey, "1", PRESENCE_TIMEOUT_HOURS, TimeUnit.HOURS);
        log.info("User {} connected to chat websocket", userId);
    }

    /**
     * 유저가 웹소켓 연결을 종료하면 OFFLINE 상태로 기록
     */
    public void disconnectUser(String userId) {
        String statusKey = PRESENCE_PREFIX + userId;
        String lastSeenKey = PRESENCE_LAST_SEEN_PREFIX + userId;
        redisTemplate.opsForValue().set(statusKey, "OFFLINE", PRESENCE_TIMEOUT_HOURS, TimeUnit.HOURS);
        redisTemplate.delete(lastSeenKey);
        log.info("User {} disconnected from chat websocket", userId);
    }

    /**
     * 특정 유저가 현재 온라인인지 확인
     */
    public boolean isUserOnline(String userId) {
        String statusKey = PRESENCE_PREFIX + userId;
        String lastSeenKey = PRESENCE_LAST_SEEN_PREFIX + userId;
        String status = redisTemplate.opsForValue().get(statusKey);
        String lastSeen = redisTemplate.opsForValue().get(lastSeenKey);
        return "ONLINE".equals(status) && lastSeen != null;
    }

    /**
     * 메시지 전송 등 활동이 있을 때 lastSeen TTL 갱신
     */
    public void touchUser(String userId) {
        String statusKey = PRESENCE_PREFIX + userId;
        String lastSeenKey = PRESENCE_LAST_SEEN_PREFIX + userId;
        // TTL 갱신
        redisTemplate.expire(statusKey, PRESENCE_TIMEOUT_HOURS, TimeUnit.HOURS);
        redisTemplate.opsForValue().set(lastSeenKey, "1", PRESENCE_TIMEOUT_HOURS, TimeUnit.HOURS);
    }
}
