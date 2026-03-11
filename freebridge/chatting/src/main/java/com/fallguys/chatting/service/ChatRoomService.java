package com.fallguys.chatting.service;

import com.fallguys.chatting.domain.ChatRoom;
import com.fallguys.chatting.api.web.dto.response.ChatRoomResponse;
import com.fallguys.chatting.repository.ChatRoomRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@Service
public class ChatRoomService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatPresenceService chatPresenceService;

    /**
     * 1:1 채팅방 생성 (이미 방이 존재할 경우 기존 방을 반환하는 로직은 추후 추가)
     */
    public ChatRoomResponse createChatRoom(List<String> participants, Map<String, String> participantNames,
            String relatedJobId, String relatedApplicationId, String relatedProposalId) {

        ChatRoom newRoom = ChatRoom.builder()
                .participants(participants)
                .participantNames(participantNames)
                .relatedJobId(relatedJobId)
                .relatedApplicationId(relatedApplicationId)
                .relatedProposalId(relatedProposalId)
                .build();

        ChatRoom savedRoom = chatRoomRepository.save(newRoom);

        return ChatRoomResponse.from(savedRoom, buildParticipantPresence(savedRoom));
    }

    /**
     * 유저가 참여 중인 활성 채팅방 목록 모두 조회
     */
    public List<ChatRoomResponse> getChatRoomsByParticipant(String participantId) {
        List<ChatRoom> rooms = chatRoomRepository.findActiveRoomsByParticipant(participantId);

        // 도메인 엔티티를 응답 DTO로 매핑하여 반환
        return rooms.stream()
                .map(room -> ChatRoomResponse.from(room, buildParticipantPresence(room)))
                .toList();
    }

    private Map<String, Boolean> buildParticipantPresence(ChatRoom room) {
        return room.getParticipants().stream()
                .collect(Collectors.toMap(Function.identity(), chatPresenceService::isUserOnline));
    }
}