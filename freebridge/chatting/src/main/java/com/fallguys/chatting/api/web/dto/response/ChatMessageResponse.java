package com.fallguys.chatting.api.web.dto.response;

import com.fallguys.chatting.domain.ChatMessage;
import com.fallguys.chatting.domain.MessageType;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Getter
@Builder
public class ChatMessageResponse {
    private String messageId;
    private String roomId;
    private String senderId;
    private String content;
    private MessageType type;
    private Map<String, Object> metadata;
    private List<String> readBy;
    private LocalDateTime createdAt;

    /**
     * Entity -> DTO 변환을 위한 정적 팩토리 메서드
     */
    public static ChatMessageResponse from(ChatMessage message) {
        return ChatMessageResponse.builder()
                .messageId(message.getId())
                .roomId(message.getRoomId())
                .senderId(message.getSenderId())
                .content(message.getContent())
                .type(message.getType())
                .metadata(message.getMetadata())
                .readBy(message.getReadBy())
                .createdAt(message.getCreatedAt())
                .build();
    }
}
