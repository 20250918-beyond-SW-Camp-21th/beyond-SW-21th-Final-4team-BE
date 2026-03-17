package com.fallguys.chatting.repository;

import com.fallguys.chatting.domain.ChatRoom;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatRoomRepository extends MongoRepository<ChatRoom, String>, ChatRoomRepositoryCustom {

    /**
     * 유저가 참여 중인 활성 채팅방 목록 조회
     * (조건: participants 배열에 아이디가 존재하고, leftBy 배열에는 존재하지 않을 것)
     */
    @Query("{ 'participants': ?0, 'leftBy': { $ne: ?0 } }")
    List<ChatRoom> findActiveRoomsByParticipant(String participantId);
}
