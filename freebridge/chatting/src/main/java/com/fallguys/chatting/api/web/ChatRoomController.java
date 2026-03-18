package com.fallguys.chatting.api.web;

import com.fallguys.chatting.api.web.dto.request.ChatRoomCreateRequest;
import com.fallguys.chatting.api.web.dto.request.ChatRoomContractUpdateRequest;
import com.fallguys.chatting.api.web.dto.response.ChatRoomResponse;
import com.fallguys.chatting.api.web.dto.response.CursorPageResponse;
import com.fallguys.chatting.api.web.dto.response.ChatMessageResponse;
import com.fallguys.chatting.security.ChatTokenProvider;
import com.fallguys.chatting.service.ChatMessageService;
import com.fallguys.chatting.service.ChatRoomService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/chat/rooms")
@RequiredArgsConstructor
public class ChatRoomController {

    private final ChatRoomService chatRoomService;
    private final ChatMessageService chatMessageService;
    private final ChatTokenProvider chatTokenProvider;

    private String extractUserId(String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return chatTokenProvider.getUserIdFromToken(authHeader.substring(7));
        }
        throw new IllegalArgumentException("유효하지 않은 인증 헤더입니다.");
    }

    @PostMapping
    public ResponseEntity<ChatRoomResponse> createRoom(
            @RequestHeader("Authorization") String authHeader,
            @Valid @RequestBody ChatRoomCreateRequest request) {

        String userId = extractUserId(authHeader);
        if (!request.getParticipants().contains(userId)) {
            throw new IllegalArgumentException("본인이 포함된 채팅방만 생성할 수 있습니다.");
        }

        ChatRoomResponse response = chatRoomService.createChatRoom(
                request.getParticipants(),
                request.getParticipantNames(),
                request.getRelatedJobId(),
                request.getRelatedApplicationId(),
                request.getRelatedProposalId(),
                request.getContractId());
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<ChatRoomResponse>> getMyRooms(
            @RequestHeader("Authorization") String authHeader) {
        String userId = extractUserId(authHeader);
        List<ChatRoomResponse> rooms = chatRoomService.getChatRoomsByParticipant(userId);
        return ResponseEntity.ok(rooms);
    }

    @PostMapping("/{roomId}/read")
    public ResponseEntity<ChatRoomResponse> markRoomAsRead(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable String roomId) {
        String userId = extractUserId(authHeader);
        ChatRoomResponse response = chatRoomService.markRoomAsRead(roomId, userId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{roomId}/leave")
    public ResponseEntity<ChatRoomResponse> leaveRoom(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable String roomId) {
        String userId = extractUserId(authHeader);
        ChatRoomResponse response = chatRoomService.leaveChatRoom(roomId, userId);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{roomId}/contract")
    public ResponseEntity<ChatRoomResponse> updateRoomContract(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable String roomId,
            @RequestBody ChatRoomContractUpdateRequest request) {
        String userId = extractUserId(authHeader);
        ChatRoomResponse response = chatRoomService.updateRoomContract(
                roomId,
                userId,
                request.getContractId(),
                Boolean.TRUE.equals(request.getOverrideExisting()));
        return ResponseEntity.ok(response);
    }

    @PostMapping(value = "/{roomId}/files", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ChatMessageResponse> uploadFileMessage(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable String roomId,
            @RequestPart("file") MultipartFile file) {
        String userId = extractUserId(authHeader);
        ChatMessageResponse response = chatMessageService.uploadFileMessage(roomId, userId, file);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{roomId}/messages")
    public ResponseEntity<CursorPageResponse<ChatMessageResponse>> getMessages(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable String roomId,
            @RequestParam(required = false) String cursorDateStr,
            @RequestParam(defaultValue = "20") int size) {

        // 인증된 사용자 식별. 방 소속 여부 등은 Service 내부 혹은 추후 방어코드 추가 가능
        String userId = extractUserId(authHeader);

        if (size <= 0 || size > 100) {
            throw new IllegalArgumentException("size는 1~100 사이여야 합니다.");
        }

        LocalDateTime cursorDate = null;
        String cursorId = null;

        if (cursorDateStr != null && !cursorDateStr.isEmpty()) {
            String[] parts = cursorDateStr.split(",", 2);
            if (parts.length > 0 && !parts[0].isEmpty()) {
                cursorDate = LocalDateTime.parse(parts[0]);
            }
            if (parts.length > 1 && !parts[1].isEmpty()) {
                cursorId = parts[1];
            }
        }

        CursorPageResponse<ChatMessageResponse> messages = chatMessageService.getPreviousMessages(roomId, cursorDate,
                cursorId, size, userId);
        return ResponseEntity.ok(messages);
    }
}
