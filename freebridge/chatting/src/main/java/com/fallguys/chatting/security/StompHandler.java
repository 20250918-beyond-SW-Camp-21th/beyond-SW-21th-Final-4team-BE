package com.fallguys.chatting.security;

import com.fallguys.chatting.service.ChatPresenceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

@Slf4j
@RequiredArgsConstructor
@Component
public class StompHandler implements ChannelInterceptor {

    private final ChatTokenProvider chatTokenProvider;
    private final ChatPresenceService chatPresenceService;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor != null) {
            StompCommand command = accessor.getCommand();

            if (StompCommand.CONNECT == command) {
                // 토큰 추출 및 검증
                String authHeader = accessor.getFirstNativeHeader("Authorization");
                if (authHeader != null && authHeader.startsWith("Bearer ")) {
                    String token = authHeader.substring(7);
                    try {
                        String userId = chatTokenProvider.getUserIdFromToken(token);
                        accessor.getSessionAttributes().put("userId", userId);

                        // Redis 에 온라인 접속 상태 기록
                        chatPresenceService.connectUser(userId);
                        log.info("WebSocket CONNECT - User ID: {}", userId);
                    } catch (Exception e) {
                        log.error("웹소켓 연결 실패: 유효하지 않은 토큰입니다.", e);
                        throw new IllegalArgumentException("유효하지 않은 STOMP 토큰입니다.");
                    }
                } else {
                    log.error("웹소켓 연결 실패: Authorization 헤더가 누락되었거나 양식이 잘못되었습니다.");
                    throw new IllegalArgumentException("WebSocket Authorization 헤더가 누락되었습니다.");
                }
            } else if (StompCommand.DISCONNECT == command) {
                String userId = (String) accessor.getSessionAttributes().get("userId");
                if (userId != null) {
                    chatPresenceService.disconnectUser(userId);
                    log.info("WebSocket DISCONNECT - User ID: {}", userId);
                }
            }
        }
        return message;
    }
}
