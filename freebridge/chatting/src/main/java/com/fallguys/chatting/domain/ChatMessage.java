package com.fallguys.chatting.domain;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Document(collection = "chat_messages")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatMessage {

    @Id
    private String id;
    private String roomId;
    private String senderId;
    private String content;

    private MessageType type;

    // 부가 정보 (예: { "contractId": 1001, "status": "SIGNED" })
    private Map<String, Object> metadata;

    private List<String> readBy = new ArrayList<>();

    @CreatedDate
    private LocalDateTime createdAt;

    @Builder
    public ChatMessage(String id, String roomId, String senderId, String content, MessageType type,
            Map<String, Object> metadata, List<String> readBy) {
        this.id = id;
        this.roomId = roomId;
        this.senderId = senderId;
        this.content = content;
        this.type = type != null ? type : MessageType.TEXT;
        this.metadata = metadata;
        this.readBy = readBy != null ? readBy : new ArrayList<>();
    }

    /**
     * 비즈니스 로직 - 객체 캡슐화
     * 상대방이 메시지를 읽었을 때 읽은 사람 목록에 추가합니다.
     */
    public void markAsReadBy(String participantId) {
        if (!this.readBy.contains(participantId)) {
            this.readBy.add(participantId);
        }
    }
}
