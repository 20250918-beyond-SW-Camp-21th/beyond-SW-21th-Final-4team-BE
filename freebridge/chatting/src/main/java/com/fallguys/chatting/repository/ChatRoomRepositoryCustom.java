package com.fallguys.chatting.repository;

import com.fallguys.chatting.domain.ChatMessage;

import java.util.Collection;

public interface ChatRoomRepositoryCustom {

    boolean clearUnreadCount(String roomId, String participantId);

    boolean updateMessageState(String roomId, String senderId, ChatMessage lastMessage,
            Collection<String> unreadRecipients);
}
