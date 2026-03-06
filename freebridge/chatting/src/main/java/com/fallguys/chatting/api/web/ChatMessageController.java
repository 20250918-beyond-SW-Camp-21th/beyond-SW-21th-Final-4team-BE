package com.fallguys.chatting.api.web;

import com.fallguys.chatting.api.web.dto.request.ChatMessageRequest;
import com.fallguys.chatting.service.ChatMessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class ChatMessageController {

    private final ChatMessageService chatMessageService;

    /**
     * 클라이언트에서 '/app/chat/message' 목적지로 패킷 전송 시 수신
     */
    @MessageMapping("/chat/message")
    public void sendMessage(ChatMessageRequest request,
            org.springframework.messaging.simp.SimpMessageHeaderAccessor headerAccessor) {
        // Prevent spoofing by extracting userId from the authenticated STOMP session
        String authenticatedUserId = null;
        if (headerAccessor != null && headerAccessor.getSessionAttributes() != null) {
            authenticatedUserId = (String) headerAccessor.getSessionAttributes().get("userId");
        }

        if (authenticatedUserId == null) {
            throw new IllegalArgumentException("인증되지 않은 사용자입니다.");
        }

        chatMessageService.sendMessage(
                request.getRoomId(),
                authenticatedUserId, // Use the authenticated ID instead of request.getSenderId()
                request.getContent(),
                request.getType(),
                request.getMetadata());
    }
}
