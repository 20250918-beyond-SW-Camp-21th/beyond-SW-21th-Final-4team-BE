package com.fallguys.chatting.api.web.dto.response;

import com.fallguys.chatting.domain.ChatRoom;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Getter
@Builder
public class ChatRoomResponse {
    private String roomId;
    private List<String> participants;
    private Map<String, String> participantNames;
    private ChatMessageResponse lastMessage;
    private Map<String, Integer> unreadCount;
    private Map<String, Boolean> participantPresence;

    private String relatedJobId;
    private String relatedApplicationId;
    private String relatedProposalId;
    private Long contractId;

    private List<String> leftBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static ChatRoomResponse from(ChatRoom room) {
        return from(room, Map.of());
    }

    public static ChatRoomResponse from(ChatRoom room, Map<String, Boolean> participantPresence) {
        return ChatRoomResponse.builder()
                .roomId(room.getId())
                .participants(room.getParticipants())
                .participantNames(room.getParticipantNames())
                .lastMessage(room.getLastMessage() != null ? ChatMessageResponse.from(room.getLastMessage()) : null)
                .unreadCount(room.getUnreadCount())
                .participantPresence(participantPresence)
                .relatedJobId(room.getRelatedJobId())
                .relatedApplicationId(room.getRelatedApplicationId())
                .relatedProposalId(room.getRelatedProposalId())
                .contractId(room.getContractId())
                .leftBy(room.getLeftBy())
                .createdAt(room.getCreatedAt())
                .updatedAt(room.getUpdatedAt())
                .build();
    }
}