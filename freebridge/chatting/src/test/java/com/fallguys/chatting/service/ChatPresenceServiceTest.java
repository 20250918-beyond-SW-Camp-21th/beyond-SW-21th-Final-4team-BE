package com.fallguys.chatting.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatPresenceServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private ChatPresenceService chatPresenceService;

    @Test
    @DisplayName("유저가 접속하면 Redis 에 ONLINE 상태로 기록된다")
    void connectUser_Success() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        chatPresenceService.connectUser("e1");

        verify(valueOperations, times(1)).set("presence:e1", "ONLINE", 1, TimeUnit.HOURS);
        verify(valueOperations, times(1)).set("presence:lastSeen:e1", "1", 1, TimeUnit.HOURS);
    }

    @Test
    @DisplayName("유저가 접속을 종료하면 Redis 에 OFFLINE 상태로 기록된다")
    void disconnectUser_Success() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        chatPresenceService.disconnectUser("e1");

        verify(valueOperations, times(1)).set("presence:e1", "OFFLINE", 1, TimeUnit.HOURS);
        verify(redisTemplate, times(1)).delete("presence:lastSeen:e1");
    }

    @Test
    @DisplayName("유저가 접속 중인지 확인한다")
    void isUserOnline_Success() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        lenient().when(valueOperations.get("presence:e1")).thenReturn("ONLINE");
        lenient().when(valueOperations.get("presence:lastSeen:e1")).thenReturn("1");
        lenient().when(valueOperations.get("presence:f1")).thenReturn("OFFLINE");
        lenient().when(valueOperations.get("presence:lastSeen:f1")).thenReturn("1");
        lenient().when(valueOperations.get("presence:f2")).thenReturn(null);
        lenient().when(valueOperations.get("presence:lastSeen:f2")).thenReturn(null);

        assertThat(chatPresenceService.isUserOnline("e1")).isTrue();
        assertThat(chatPresenceService.isUserOnline("f1")).isFalse();
        assertThat(chatPresenceService.isUserOnline("f2")).isFalse();
    }
}
