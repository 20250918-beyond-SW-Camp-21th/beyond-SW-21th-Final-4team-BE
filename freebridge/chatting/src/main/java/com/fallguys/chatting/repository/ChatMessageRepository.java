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

    // 커서 기반 페이징: 입력된 커서(시간) 이전의 메시지 조회
    List<ChatMessage> findByRoomIdAndCreatedAtLessThanOrderByCreatedAtDesc(String roomId, LocalDateTime cursorDate,
            Pageable pageable);
}
