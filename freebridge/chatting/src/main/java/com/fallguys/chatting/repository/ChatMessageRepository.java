package com.fallguys.chatting.repository;

import com.fallguys.chatting.domain.ChatMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ChatMessageRepository extends MongoRepository<ChatMessage, String> {

    // 최신 메시지 조회용 (페이징 커서가 없는 최초 로딩 시)
    List<ChatMessage> findByRoomIdOrderByCreatedAtDesc(String roomId, Pageable pageable);

    // 단일 커서 기반 페이징 (하위 호환성을 위해 유지)
    List<ChatMessage> findByRoomIdAndCreatedAtLessThanOrderByCreatedAtDesc(String roomId, LocalDateTime cursorDate,
            Pageable pageable);

    // 복합 커서 기반 페이징 (createdAt + id)
    @org.springframework.data.mongodb.repository.Query(
            value = "{ 'roomId': ?0, '$or': [ { 'createdAt': { $lt: ?1 } }, { 'createdAt': ?1, '_id': { $lt: ?2 } } ] }",
            sort = "{ 'createdAt': -1, '_id': -1 }"
    )
    List<ChatMessage> findByRoomIdAndCursor(String roomId, LocalDateTime cursorDate, String cursorId,
            Pageable pageable);
}
