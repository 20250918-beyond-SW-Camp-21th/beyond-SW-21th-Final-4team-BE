package com.fallguys.chatting.controller;

import com.fallguys.chatting.dto.ChatRoomCreateRequest;
import com.fallguys.chatting.dto.ChatRoomResponse;
import com.fallguys.chatting.dto.CursorPageResponse;
import com.fallguys.chatting.dto.ChatMessageResponse;
import com.fallguys.chatting.security.ChatTokenProvider;
import com.fallguys.chatting.service.ChatMessageService;
import com.fallguys.chatting.service.ChatRoomService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @Valid @RequestBody ChatRoomCreateRequest request) {

        if (authHeader != null) {
            String userId = extractUserId(authHeader);
            if (!request.getParticipants().contains(userId)) {
                throw new IllegalArgumentException("본인이 포함된 채팅방만 생성할 수 있습니다.");
            }
        }

        ChatRoomResponse response = chatRoomService.createChatRoom(
                request.getParticipants(),
                request.getParticipantNames(),
                request.getRelatedJobId(),
                request.getRelatedApplicationId(),
                request.getRelatedProposalId());
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<ChatRoomResponse>> getMyRooms(
            @RequestHeader("Authorization") String authHeader) {
        String userId = extractUserId(authHeader);
        List<ChatRoomResponse> rooms = chatRoomService.getChatRoomsByParticipant(userId);
        return ResponseEntity.ok(rooms);
    }

    @GetMapping("/{roomId}/messages")
    public ResponseEntity<CursorPageResponse<ChatMessageResponse>> getMessages(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable String roomId,
            @RequestParam(required = false) String cursorDateStr,
            @RequestParam(defaultValue = "20") int size) {

        // 인증된 사용자 식별. 방 소속 여부 등은 Service 내부 혹은 추후 방어코드 추가 가능
        String userId = extractUserId(authHeader);

        LocalDateTime cursorDate = null;
        if (cursorDateStr != null && !cursorDateStr.isEmpty()) {
            cursorDate = LocalDateTime.parse(cursorDateStr);
        }

        CursorPageResponse<ChatMessageResponse> messages = chatMessageService.getPreviousMessages(roomId, cursorDate,
                size);
        return ResponseEntity.ok(messages);
    }
}
