package com.fallguys.chatting.repository;

import com.fallguys.chatting.domain.ChatMessage;
import com.fallguys.chatting.domain.ChatRoom;

import java.util.Collection;
import java.util.Optional;

public interface ChatRoomRepositoryCustom {

    Optional<ChatRoom> findRoomByContractAndParticipants(Long contractId, Collection<String> participants);

    boolean clearUnreadCount(String roomId, String participantId);

    boolean updateMessageState(String roomId, String senderId, ChatMessage lastMessage,
            Collection<String> unreadRecipients);
}
