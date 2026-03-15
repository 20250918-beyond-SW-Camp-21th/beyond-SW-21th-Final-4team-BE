package com.fallguys.chatting.domain;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Document(collection = "chat_rooms")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatRoom {

    @Id
    private String id;

    // 참여자 원본 ID 배열 (e.g. "e1", "f1")
    private List<String> participants = new ArrayList<>();

    // Map 구조의 참여자 이름 캐시 (e.g. {"e1": "스타트업 A", "f1": "김프론트"})
    private Map<String, String> participantNames = new HashMap<>();

    // 마지막 메시지
    private ChatMessage lastMessage;

    // Map 구조의 안 읽은 카운트 (e.g. {"e1": 1, "f1": 0})
    private Map<String, Integer> unreadCount = new HashMap<>();

    // 해당 채팅방이 파생된 공고/지원/제안/계약 식별자
    private String relatedJobId;
    private String relatedApplicationId;
    private String relatedProposalId;
    private Long contractId;

    // 방을 나간 유저 목록 배열
    private List<String> leftBy = new ArrayList<>();

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    @Builder
    public ChatRoom(String id, List<String> participants, Map<String, String> participantNames,
            String relatedJobId, String relatedApplicationId, String relatedProposalId, Long contractId) {
        this.id = id;
        this.participants = participants != null ? participants : new ArrayList<>();
        this.participantNames = participantNames != null ? participantNames : new HashMap<>();
        this.relatedJobId = relatedJobId;
        this.relatedApplicationId = relatedApplicationId;
        this.relatedProposalId = relatedProposalId;
        this.contractId = contractId;

        // 방 생성 시 초기 안 읽은 메시지 개수는 0으로 세팅
        if (this.participants != null) {
            this.participants.forEach(p -> this.unreadCount.put(p, 0));
        }
    }

    /*
     * 비즈니스 로직 - 객체 캡슐화 (POJO 지향)
     */

    // 1. 채팅방에 새 메시지 수신 시 마지막 메시지 갱신
    public void updateLastMessage(ChatMessage message) {
        this.lastMessage = message;
        this.updatedAt = LocalDateTime.now();
    }

    // 2. 메시지를 보냈을 때 상대방들의 안 읽은 메시지 수를 증가
    public void incrementUnreadCountForOthers(String senderId) {
        for (String participant : participants) {
            if (!participant.equals(senderId)) {
                this.unreadCount.put(participant, this.unreadCount.getOrDefault(participant, 0) + 1);
            }
        }
    }

    // 3. 방에 들어오거나 메시지를 읽었을 때 카운트 초기화
    public void resetUnreadCount(String participantId) {
        this.unreadCount.put(participantId, 0);
    }

    // 4. 유저가 방을 나감 처리
    public void leave(String participantId) {
        if (!leftBy.contains(participantId)) {
            leftBy.add(participantId);
        }
    }

    public boolean isParticipant(String participantId) {
        return participantId != null && participants.contains(participantId);
    }

    public boolean isEmployerParticipant(String participantId) {
        return isParticipant(participantId) && participantId.toLowerCase().startsWith("e");
    }

    // 5. 모두가 방을 나갔는지 확인
    public boolean hasEveryoneLeft() {
        return leftBy.containsAll(participants);
    }

    // 6. 새로운 계약이 성사되었을 때 연결
    public void updateContractId(Long contractId) {
        if (contractId == null) {
            throw new IllegalArgumentException("연결할 계약 ID가 필요합니다.");
        }
        if (this.contractId != null && !this.contractId.equals(contractId)) {
            throw new IllegalArgumentException("이미 계약이 연결된 채팅방입니다. 기존 계약을 덮어쓸 수 없습니다.");
        }
        this.contractId = contractId;
    }
}
