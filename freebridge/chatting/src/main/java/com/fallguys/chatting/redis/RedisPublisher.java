package com.fallguys.chatting.redis;

import com.fallguys.chatting.api.web.dto.response.ChatMessageResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.stereotype.Service;

@Slf4j
@RequiredArgsConstructor
@Service
public class RedisPublisher {

    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * 채팅 메시지를 Redis Topic 으로 발행 (Publish)
     */
    public void publish(ChannelTopic topic, ChatMessageResponse message) {
        redisTemplate.convertAndSend(topic.getTopic(), message);
    }
}
