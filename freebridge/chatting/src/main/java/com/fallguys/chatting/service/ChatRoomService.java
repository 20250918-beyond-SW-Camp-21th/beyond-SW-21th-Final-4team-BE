package com.fallguys.chatting.service;

import com.fallguys.chatting.api.web.dto.response.ChatRoomResponse;
import com.fallguys.chatting.domain.ChatRoom;
import com.fallguys.chatting.repository.ChatRoomRepository;
import com.fallguys.chatting.repository.UnreadMessageRedisRepository;
import com.fallguys.common.api.contract.ContractQuery;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@Service
public class ChatRoomService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatPresenceService chatPresenceService;
    private final ChatMessageService chatMessageService;
    private final ContractQuery contractQuery;
    private final UnreadMessageRedisRepository unreadMessageRedisRepository;

    /**
     * 1:1 채팅방 생성
     */
    public ChatRoomResponse createChatRoom(List<String> participants, Map<String, String> participantNames,
            String relatedJobId, String relatedApplicationId, String relatedProposalId, Long contractId) {

        if (contractId != null && !contractQuery.existsContract(contractId)) {
            log.warn("존재하지 않는 계약 기준 채팅방 생성 시도 - contractId: {}, participants: {}", contractId, participants);
            throw new NoSuchElementException("계약을 찾을 수 없습니다.");
        }

        if (contractId != null) {
            ChatRoom existingContractRoom = chatRoomRepository
                    .findRoomByContractAndParticipants(contractId, participants)
                    .orElse(null);
            if (existingContractRoom != null) {
                return ChatRoomResponse.from(existingContractRoom, buildParticipantPresence(existingContractRoom));
            }
        }

        ChatRoom newRoom = ChatRoom.builder()
                .participants(participants)
                .participantNames(participantNames)
                .relatedJobId(relatedJobId)
                .relatedApplicationId(relatedApplicationId)
                .relatedProposalId(relatedProposalId)
                .contractId(contractId)
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

    public ChatRoomResponse markRoomAsRead(String roomId, String participantId) {
        ChatRoom room = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("채팅방을 찾을 수 없습니다: " + roomId));

        if (!room.isParticipant(participantId)) {
            log.warn("권한 없는 사용자의 채팅방 읽음 처리 시도 - roomId: {}, participantId: {}", roomId, participantId);
            throw new IllegalArgumentException("채팅방에 참여하고 있지 않습니다.");
        }

        if (!chatRoomRepository.clearUnreadCount(roomId, participantId)) {
            throw new IllegalStateException("읽음 상태를 갱신할 수 없습니다: " + roomId);
        }
        unreadMessageRedisRepository.resetUnreadCount(roomId, participantId);

        ChatRoom refreshedRoom = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalStateException("읽음 상태가 갱신된 채팅방을 다시 찾을 수 없습니다: " + roomId));
        return ChatRoomResponse.from(refreshedRoom, buildParticipantPresence(refreshedRoom));
    }

    public ChatRoomResponse leaveChatRoom(String roomId, String participantId) {
        ChatRoom room = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("채팅방을 찾을 수 없습니다: " + roomId));

        if (!room.getParticipants().contains(participantId)) {
            log.warn("권한 없는 사용자의 채팅방 나가기 시도 - roomId: {}, participantId: {}", roomId, participantId);
            throw new IllegalArgumentException("채팅방에 참여하고 있지 않습니다.");
        }

        boolean alreadyLeft = room.getLeftBy().contains(participantId);
        room.leave(participantId);
        ChatRoom savedRoom = chatRoomRepository.save(room);
        if (!alreadyLeft) {
            chatMessageService.publishLeaveSystemMessage(savedRoom, participantId);
        }
        return ChatRoomResponse.from(savedRoom, buildParticipantPresence(savedRoom));
    }

    public ChatRoomResponse updateRoomContract(String roomId, String participantId, Long contractId,
            boolean overrideExisting) {
        ChatRoom room = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("채팅방을 찾을 수 없습니다: " + roomId));

        if (!room.isParticipant(participantId)) {
            log.warn("권한 없는 사용자의 채팅방 계약 연결 시도 - roomId: {}, participantId: {}", roomId, participantId);
            throw new IllegalArgumentException("채팅방에 참여하고 있지 않습니다.");
        }
        if (!room.isEmployerParticipant(participantId)) {
            log.warn("기업 회원이 아닌 사용자의 채팅방 계약 연결 시도 - roomId: {}, participantId: {}", roomId, participantId);
            throw new IllegalArgumentException("기업 회원만 채팅방에 계약을 연결할 수 있습니다.");
        }
        if (contractId == null) {
            log.warn("계약 ID 없이 채팅방 계약 연결 시도 - roomId: {}, participantId: {}", roomId, participantId);
            throw new IllegalArgumentException("연결할 계약 ID가 필요합니다.");
        }
        if (room.getContractId() != null && !room.getContractId().equals(contractId) && !overrideExisting) {
            log.warn(
                    "기존 계약이 연결된 채팅방 덮어쓰기 시도 - roomId: {}, participantId: {}, currentContractId: {}, requestedContractId: {}",
                    roomId,
                    participantId,
                    room.getContractId(),
                    contractId);
            throw new IllegalArgumentException("이미 계약이 연결된 채팅방입니다. 기존 계약을 덮어쓸 수 없습니다.");
        }
        if (room.getContractId() != null && !room.getContractId().equals(contractId) && overrideExisting) {
            log.info(
                    "명시적 계약 재연결 수행 - roomId: {}, participantId: {}, previousContractId: {}, requestedContractId: {}",
                    roomId,
                    participantId,
                    room.getContractId(),
                    contractId);
        }

        if (!contractQuery.existsContract(contractId)) {
            log.warn("존재하지 않는 계약 연결 시도 - roomId: {}, participantId: {}, contractId: {}", roomId, participantId,
                    contractId);
            throw new NoSuchElementException("계약을 찾을 수 없습니다.");
        }

        room.updateContractId(contractId);
        ChatRoom savedRoom = chatRoomRepository.save(room);
        return ChatRoomResponse.from(savedRoom, buildParticipantPresence(savedRoom));
    }

    private Map<String, Boolean> buildParticipantPresence(ChatRoom room) {
        return room.getParticipants().stream()
                .collect(Collectors.toMap(Function.identity(), chatPresenceService::isUserOnline));
    }
}
