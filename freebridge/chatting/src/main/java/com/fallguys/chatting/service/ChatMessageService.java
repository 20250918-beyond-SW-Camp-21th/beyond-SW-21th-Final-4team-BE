package com.fallguys.chatting.service;

import com.fallguys.chatting.domain.ChatMessage;
import com.fallguys.chatting.domain.ChatRoom;
import com.fallguys.chatting.domain.MessageType;
import com.fallguys.chatting.api.web.dto.response.ChatMessageResponse;
import com.fallguys.chatting.api.web.dto.response.CursorPageResponse;
import com.fallguys.chatting.redis.RedisPublisher;
import com.fallguys.chatting.repository.ChatMessageRepository;
import com.fallguys.chatting.repository.ChatRoomRepository;
import com.fallguys.chatting.repository.UnreadMessageRedisRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Slf4j
@RequiredArgsConstructor
@Service
public class ChatMessageService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final UnreadMessageRedisRepository unreadMessageRedisRepository;
    private final RedisPublisher redisPublisher;
    private final ChannelTopic channelTopic;
    private final ChatPresenceService chatPresenceService;

    /**
     * 클라이언트로부터 메시지 수신 시 처리
     */
    public ChatMessageResponse sendMessage(String roomId, String senderId, String content, MessageType type,
            Map<String, Object> metadata) {

        ChatRoom room = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("채팅방을 찾을 수 없습니다: " + roomId));

        // 보안 체크: senderId가 해당 방의 참여자인지 확인
        if (!room.getParticipants().contains(senderId)) {
            log.warn("권한 없는 사용자의 메시지 전송 시도 - roomId: {}, senderId: {}", roomId, senderId);
            throw new IllegalArgumentException("채팅방에 참여하고 있지 않습니다.");
        }
        if (room.getLeftBy().contains(senderId)) {
            log.warn("채팅방을 나간 사용자의 메시지 전송 시도 - roomId: {}, senderId: {}", roomId, senderId);
            throw new IllegalArgumentException("채팅방을 나간 후에는 메시지를 전송할 수 없습니다.");
        }

        // 1. 메시지 도메인 객체 생성
        ChatMessage chatMessage = ChatMessage.builder()
                .roomId(roomId)
                .senderId(senderId)
                .content(content)
                .type(type)
                .metadata(metadata)
                .build();

        // 본인 읽음 처리
        chatMessage.markAsReadBy(senderId);

        // 2. MongoDB 저장
        ChatMessage savedMessage = chatMessageRepository.save(chatMessage);

        List<String> unreadRecipients = room.getParticipants().stream()
                .filter(participantId -> !participantId.equals(senderId))
                .toList();

        if (!chatRoomRepository.updateMessageState(roomId, senderId, savedMessage, unreadRecipients)) {
            throw new IllegalStateException("메시지 상태를 채팅방에 반영할 수 없습니다: " + roomId);
        }

        unreadRecipients.forEach(participantId -> unreadMessageRedisRepository.incrementUnreadCount(roomId, participantId));

        // 5. Response DTO 생성
        ChatMessageResponse response = ChatMessageResponse.from(savedMessage);

        // 활동 기반 presence 갱신 (예외 발생 시 메시지 전송 실패 방지를 위해 try-catch 처리)
        try {
            chatPresenceService.touchUser(senderId);
        } catch (Exception e) {
            log.error("Failed to update presence for user: {}", senderId, e);
        }

        redisPublisher.publish(channelTopic, response);

        return response;
    }

    public ChatMessageResponse publishLeaveSystemMessage(ChatRoom room, String participantId) {
        String leaverName = room.getParticipantNames().getOrDefault(participantId, participantId);

        ChatMessage chatMessage = ChatMessage.builder()
                .roomId(room.getId())
                .senderId(participantId)
                .content(leaverName + "님이 채팅방을 나갔습니다.")
                .type(MessageType.SYSTEM)
                .metadata(Map.of(
                        "eventType", "ROOM_LEFT",
                        "participantId", participantId))
                .build();

        chatMessage.markAsReadBy(participantId);

        ChatMessage savedMessage = chatMessageRepository.save(chatMessage);

        List<String> unreadRecipients = room.getParticipants().stream()
                .filter(roomParticipantId -> !roomParticipantId.equals(participantId))
                .toList();

        if (!chatRoomRepository.updateMessageState(room.getId(), participantId, savedMessage, unreadRecipients)) {
            throw new IllegalStateException("퇴장 메시지 상태를 채팅방에 반영할 수 없습니다: " + room.getId());
        }

        unreadRecipients
                .forEach(roomParticipantId -> unreadMessageRedisRepository.incrementUnreadCount(room.getId(), roomParticipantId));

        ChatMessageResponse response = ChatMessageResponse.from(savedMessage);
        redisPublisher.publish(channelTopic, response);
        return response;
    }

    /**
     * 커서 기반 페이징으로 이전 메시지 목록 무한 스크롤 조회 (복합 커서 적용)
     */
    public CursorPageResponse<ChatMessageResponse> getPreviousMessages(String roomId,
            java.time.LocalDateTime cursorDate, String cursorId, int size, String userId) {

        ChatRoom room = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("채팅방을 찾을 수 없습니다: " + roomId));

        // 보안 체크: 메시지를 요청하는 유저가 해당 방의 참여자인지 확인
        if (!room.getParticipants().contains(userId)) {
            log.warn("권한 없는 사용자의 이전 메시지 조회 시도 - roomId: {}, userId: {}", roomId, userId);
            throw new IllegalArgumentException("채팅방에 접근할 권한이 없습니다.");
        }

        if (!chatRoomRepository.clearUnreadCount(roomId, userId)) {
            throw new IllegalStateException("읽음 상태를 갱신할 수 없습니다: " + roomId);
        }
        unreadMessageRedisRepository.resetUnreadCount(roomId, userId);

        org.springframework.data.domain.Sort sort = org.springframework.data.domain.Sort.by(
                org.springframework.data.domain.Sort.Order.desc("createdAt"),
                org.springframework.data.domain.Sort.Order.desc("_id"));
        org.springframework.data.domain.PageRequest pageRequest = org.springframework.data.domain.PageRequest.of(0,
                size, sort);
        java.util.List<ChatMessage> messages;

        if (cursorDate == null) {
            // 처음 진입: 가장 최근 메시지부터 size 만큼 조회
            messages = chatMessageRepository.findByRoomIdOrderByCreatedAtDesc(roomId, pageRequest);
        } else {
            // 이후 페이징: cursorDate 및 cursorId 복합 커서 기반 조회
            if (cursorId == null || cursorId.isEmpty()) {
                // 하위 호환성 (cursorId 없을 때)
                messages = chatMessageRepository.findByRoomIdAndCreatedAtLessThanOrderByCreatedAtDesc(roomId,
                        cursorDate,
                        pageRequest);
            } else {
                messages = chatMessageRepository.findByRoomIdAndCursor(roomId, cursorDate, cursorId, pageRequest);
            }
        }

        java.util.List<ChatMessageResponse> itemResponses = messages.stream()
                .map(ChatMessageResponse::from)
                .toList();

        String nextCursor = null;
        boolean hasNext = false;
        if (!messages.isEmpty()) {
            ChatMessage lastExtractedMsg = messages.get(messages.size() - 1);
            if (lastExtractedMsg != null) {
                // 커서는 "생성시간,메시지ID" 형태의 복합 커서 문자열로 통일
                if (lastExtractedMsg.getCreatedAt() != null && lastExtractedMsg.getId() != null) {
                    nextCursor = lastExtractedMsg.getCreatedAt().toString() + "," + lastExtractedMsg.getId();
                } else if (lastExtractedMsg.getCreatedAt() != null) {
                    nextCursor = lastExtractedMsg.getCreatedAt().toString();
                }
                hasNext = messages.size() >= size; // size 만큼 가져왔으면 다음 페이지가 있을 확률이 큼
            }
        }

        return CursorPageResponse.<ChatMessageResponse>builder()
                .items(itemResponses)
                .nextCursor(nextCursor)
                .hasNext(hasNext)
                .build();
    }
}
