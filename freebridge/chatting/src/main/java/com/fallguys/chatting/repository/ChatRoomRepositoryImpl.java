package com.fallguys.chatting.repository;

import com.fallguys.chatting.domain.ChatMessage;
import com.fallguys.chatting.domain.ChatRoom;
import com.mongodb.client.result.UpdateResult;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import java.time.LocalDateTime;
import java.util.Collection;

@RequiredArgsConstructor
public class ChatRoomRepositoryImpl implements ChatRoomRepositoryCustom {

    private final MongoTemplate mongoTemplate;

    @Override
    public boolean clearUnreadCount(String roomId, String participantId) {
        Query query = new Query(Criteria.where("_id").is(roomId).and("participants").is(participantId));
        Update update = new Update().set("unreadCount." + participantId, 0);
        UpdateResult result = mongoTemplate.updateFirst(query, update, ChatRoom.class);
        return result.getMatchedCount() > 0;
    }

    @Override
    public boolean updateMessageState(String roomId, String senderId, ChatMessage lastMessage,
            Collection<String> unreadRecipients) {
        Query query = new Query(Criteria.where("_id").is(roomId).and("participants").is(senderId));
        Update update = new Update()
                .set("lastMessage", lastMessage)
                .set("updatedAt", LocalDateTime.now());

        unreadRecipients.forEach(participantId -> update.inc("unreadCount." + participantId, 1));

        UpdateResult result = mongoTemplate.updateFirst(query, update, ChatRoom.class);
        return result.getMatchedCount() > 0;
    }
}
