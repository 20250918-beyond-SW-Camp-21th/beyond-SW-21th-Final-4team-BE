package com.fallguys.chatting.service;

import com.fallguys.common.port.FileStorage;
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
import org.springframework.http.MediaType;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@Service
public class ChatMessageService {

    private static final long MAX_CHAT_FILE_BYTES = 20 * 1024 * 1024; // 20MB
    private static final Set<String> ALLOWED_CHAT_FILE_TYPES = Set.of(
            MediaType.APPLICATION_PDF_VALUE,
            MediaType.TEXT_PLAIN_VALUE,
            "application/zip",
            "application/x-zip-compressed",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.ms-excel",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "application/vnd.ms-powerpoint",
            "application/vnd.openxmlformats-officedocument.presentationml.presentation");

    private final ChatRoomRepository chatRoomRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final UnreadMessageRedisRepository unreadMessageRedisRepository;
    private final RedisPublisher redisPublisher;
    private final ChannelTopic channelTopic;
    private final ChatPresenceService chatPresenceService;
    private final FileStorage fileStorage;

    /**
     * 클라이언트로부터 메시지 수신 시 처리
     */
    public ChatMessageResponse sendMessage(String roomId, String senderId, String content, MessageType type,
            Map<String, Object> metadata) {

        ChatRoom room = findRoomOrThrow(roomId);
        validateParticipant(room, senderId, true);
        validateMessageContent(type, content);

        // 1. 메시지 도메인 객체 생성
        ChatMessage chatMessage = ChatMessage.builder()
                .roomId(roomId)
                .senderId(senderId)
                .content(content)
                .type(type)
                .metadata(normalizeMetadataForPersistence(type, metadata))
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
        ChatMessageResponse response = toResponse(savedMessage);

        // 활동 기반 presence 갱신 (예외 발생 시 메시지 전송 실패 방지를 위해 try-catch 처리)
        try {
            chatPresenceService.touchUser(senderId);
        } catch (Exception e) {
            log.error("Failed to update presence for user: {}", senderId, e);
        }

        redisPublisher.publish(channelTopic, response);

        return response;
    }

    public ChatMessageResponse uploadFileMessage(String roomId, String senderId, MultipartFile file) {
        ChatRoom room = findRoomOrThrow(roomId);
        validateParticipant(room, senderId, true);
        validateUploadableFile(file);

        String uploadKey = null;
        try {
            byte[] fileBytes = file.getBytes();
            String originalFileName = resolveOriginalFileName(file.getOriginalFilename());
            String contentType = file.getContentType();
            String safeExtension = extractExtension(originalFileName);
            uploadKey = "chat/rooms/" + roomId + "/" + UUID.randomUUID() + safeExtension;

            String storedKey = fileStorage.upload(fileBytes, uploadKey, contentType);
            Map<String, Object> metadata = Map.of(
                    "fileKey", storedKey,
                    "fileName", originalFileName,
                    "fileSize", file.getSize(),
                    "contentType", contentType);

            return sendMessage(roomId, senderId, originalFileName, MessageType.FILE, metadata);
        } catch (IOException e) {
            log.error("채팅 파일 업로드 실패 - roomId: {}, senderId: {}, fileName: {}", roomId, senderId,
                    file.getOriginalFilename(), e);
            throw new IllegalArgumentException("파일을 읽을 수 없습니다.");
        } catch (RuntimeException e) {
            if (uploadKey != null) {
                try {
                    fileStorage.deleteByKey(uploadKey);
                } catch (Exception deleteEx) {
                    log.error("채팅 업로드 롤백 삭제 실패 - key: {}", uploadKey, deleteEx);
                }
            }
            throw e;
        }
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

        ChatMessageResponse response = toResponse(savedMessage);
        redisPublisher.publish(channelTopic, response);
        return response;
    }

    /**
     * 커서 기반 페이징으로 이전 메시지 목록 무한 스크롤 조회 (복합 커서 적용)
     */
    public CursorPageResponse<ChatMessageResponse> getPreviousMessages(String roomId,
            java.time.LocalDateTime cursorDate, String cursorId, int size, String userId) {

        ChatRoom room = findRoomOrThrow(roomId);
        validateParticipant(room, userId, false);

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
                .map(this::toResponse)
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

    private ChatRoom findRoomOrThrow(String roomId) {
        return chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("채팅방을 찾을 수 없습니다: " + roomId));
    }

    private void validateParticipant(ChatRoom room, String participantId, boolean requireActiveParticipant) {
        if (!room.getParticipants().contains(participantId)) {
            log.warn("권한 없는 사용자의 채팅 접근 시도 - roomId: {}, participantId: {}", room.getId(), participantId);
            throw new IllegalArgumentException("채팅방에 접근할 권한이 없습니다.");
        }

        if (requireActiveParticipant && room.getLeftBy() != null && room.getLeftBy().contains(participantId)) {
            log.warn("채팅방을 나간 사용자의 접근 시도 - roomId: {}, participantId: {}", room.getId(), participantId);
            throw new IllegalArgumentException("채팅방을 나간 후에는 메시지를 전송할 수 없습니다.");
        }
    }

    private void validateMessageContent(MessageType type, String content) {
        if (type == MessageType.SYSTEM) {
            return;
        }

        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("메시지 내용은 비어 있을 수 없습니다.");
        }
    }

    private void validateUploadableFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("업로드할 파일이 없습니다.");
        }

        if (file.getSize() > MAX_CHAT_FILE_BYTES) {
            throw new IllegalArgumentException("채팅 파일은 20MB 이하만 업로드할 수 있습니다.");
        }

        String contentType = file.getContentType();
        if (contentType == null || contentType.isBlank()) {
            throw new IllegalArgumentException("파일 형식을 확인할 수 없습니다.");
        }

        String normalizedType = contentType.toLowerCase();
        if (!normalizedType.startsWith("image/") && !ALLOWED_CHAT_FILE_TYPES.contains(normalizedType)) {
            throw new IllegalArgumentException("지원하지 않는 파일 형식입니다.");
        }
    }

    private ChatMessageResponse toResponse(ChatMessage message) {
        return ChatMessageResponse.builder()
                .messageId(message.getId())
                .roomId(message.getRoomId())
                .senderId(message.getSenderId())
                .content(message.getContent())
                .type(message.getType())
                .metadata(enrichMetadataForResponse(message.getType(), message.getMetadata()))
                .readBy(message.getReadBy())
                .createdAt(message.getCreatedAt())
                .build();
    }

    private Map<String, Object> normalizeMetadataForPersistence(MessageType type, Map<String, Object> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return metadata;
        }

        Map<String, Object> normalized = new HashMap<>(metadata);
        if (type == MessageType.FILE) {
            normalized.remove("fileUrl");
            normalized.remove("downloadUrl");
        }
        return normalized;
    }

    private Map<String, Object> enrichMetadataForResponse(MessageType type, Map<String, Object> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return metadata;
        }

        Map<String, Object> enriched = new HashMap<>(metadata);
        if (type == MessageType.FILE) {
            String fileKey = extractMetadataString(enriched.get("fileKey"));
            String contentType = extractMetadataString(enriched.get("contentType"));
            String fileName = extractMetadataString(enriched.get("fileName"));
            if (fileKey != null) {
                try {
                    String fileUrl = isInlinePreviewableFile(contentType)
                            ? fileStorage.generatePresignedUrl(fileKey)
                            : fileStorage.generatePresignedDownloadUrl(fileKey, fileName);
                    enriched.put("fileUrl", fileUrl);
                } catch (Exception e) {
                    log.warn("채팅 파일 presigned url 생성 실패 - key: {}", fileKey, e);
                }
            }
        }
        return enriched;
    }

    private boolean isInlinePreviewableFile(String contentType) {
        if (contentType == null || contentType.isBlank()) {
            return false;
        }

        String normalizedType = contentType.toLowerCase();
        return normalizedType.startsWith("image/") || MediaType.APPLICATION_PDF_VALUE.equals(normalizedType);
    }

    private String extractMetadataString(Object value) {
        if (value == null) {
            return null;
        }

        String normalized = String.valueOf(value).trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private String resolveOriginalFileName(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return "attachment";
        }

        String normalized = fileName.replace("\\", "_").replace("/", "_").trim();
        return normalized.isEmpty() ? "attachment" : normalized;
    }

    private String extractExtension(String fileName) {
        if (fileName == null) {
            return "";
        }

        int extensionIndex = fileName.lastIndexOf('.');
        if (extensionIndex < 0 || extensionIndex == fileName.length() - 1) {
            return "";
        }

        String extension = fileName.substring(extensionIndex);
        return extension.length() > 16 ? "" : extension;
    }
}
