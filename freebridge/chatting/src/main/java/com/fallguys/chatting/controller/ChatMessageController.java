package com.fallguys.chatting.controller;

import com.fallguys.chatting.dto.ChatMessageRequest;
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
    public void sendMessage(ChatMessageRequest request) {
        chatMessageService.sendMessage(
                request.getRoomId(),
                request.getSenderId(),
                request.getContent(),
                request.getType(),
                request.getMetadata());
    }
}
