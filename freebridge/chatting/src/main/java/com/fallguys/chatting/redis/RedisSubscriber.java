package com.fallguys.chatting.redis;

import com.fallguys.chatting.api.web.dto.response.ChatMessageResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Service;

@Slf4j
@RequiredArgsConstructor
@Service
public class RedisSubscriber implements MessageListener {

    private final ObjectMapper objectMapper;
    private final RedisTemplate<String, Object> redisTemplate;
    private final SimpMessageSendingOperations messagingTemplate;

    /**
     * Redis 에서 메시지가 publish 되면 대기하고 있던 이 메소드가 불립니다.
     */
    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            // pub/sub 에서 발행된 데이터를 직렬화 컨버팅
            String publishMessage = redisTemplate.getStringSerializer().deserialize(message.getBody());
            ChatMessageResponse roomMessage = objectMapper.readValue(publishMessage, ChatMessageResponse.class);

            // WebSocket 구독자에게 STOMP 채널을 통해 전달
            messagingTemplate.convertAndSend("/topic/chat/room/" + roomMessage.getRoomId(), roomMessage);
        } catch (Exception e) {
            log.error("Redis Subscribe Exception", e);
        }
    }
}
