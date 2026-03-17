package com.fallguys.chatting.service;

import com.fallguys.chatting.domain.ChatMessage;
import com.fallguys.chatting.domain.ChatRoom;
import com.fallguys.chatting.domain.MessageType;
import com.fallguys.chatting.api.web.dto.response.ChatMessageResponse;
import com.fallguys.chatting.api.web.dto.response.CursorPageResponse;
import com.fallguys.chatting.redis.RedisPublisher;
import com.fallguys.chatting.repository.ChatMessageRepository;
import com.fallguys.chatting.repository.ChatRoomRepository;
import com.fallguys.chatting.repository.UnreadMessageRedisRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.listener.ChannelTopic;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatMessageServiceTest {

        @Mock
        private ChatMessageRepository chatMessageRepository;
        @Mock
        private ChatRoomRepository chatRoomRepository;
        @Mock
        private UnreadMessageRedisRepository unreadMessageRedisRepository;
        @Mock
        private RedisPublisher redisPublisher;
        @Mock
        private ChannelTopic channelTopic;

        @InjectMocks
        private ChatMessageService chatMessageService; // 구현 필요

        @Test
        @DisplayName("메시지를 전송하고 Redis 로 Publish 한다")
        void sendMessage_Success() {
                // given
                ChatRoom room = ChatRoom.builder().id("room1").participants(List.of("e1", "f1")).build();
                when(chatRoomRepository.findById("room1")).thenReturn(Optional.of(room));

                ChatMessage savedMsg = ChatMessage.builder()
                                .id("msg1")
                                .roomId("room1")
                                .senderId("e1")
                                .content("안녕하세요")
                                .type(MessageType.TEXT)
                                .build();

                when(chatMessageRepository.save(any(ChatMessage.class))).thenReturn(savedMsg);
                when(chatRoomRepository.updateMessageState(eq("room1"), eq("e1"), eq(savedMsg), anyList()))
                                .thenReturn(true);

                // when
                ChatMessageResponse response = chatMessageService.sendMessage("room1", "e1", "안녕하세요", MessageType.TEXT,
                                null);

                // then
                assertThat(response.getContent()).isEqualTo("안녕하세요");

                // Redis Publisher 가 불렸는지 검증
                verify(redisPublisher, times(1)).publish(any(), any());
                verify(chatRoomRepository, times(1)).updateMessageState(eq("room1"), eq("e1"), eq(savedMsg), anyList());

                // 안 읽은 메시지 수 캐시 증가가 불렸는지 검증 (수신자 f1에 대해)
                verify(unreadMessageRedisRepository, times(1)).incrementUnreadCount("room1", "f1");
        }

        @Test
        @DisplayName("커서 기반 페이징으로 이전 메시지 목록을 불러온다")
        void getPreviousMessages_Success() {
                // given
                ChatRoom room = ChatRoom.builder().id("room1").participants(List.of("e1", "f1")).build();
                when(chatRoomRepository.findById("room1")).thenReturn(Optional.of(room));

                ChatMessage msg1 = ChatMessage.builder().id("msg1").roomId("room1").content("M1").build();
                ChatMessage msg2 = ChatMessage.builder().id("msg2").roomId("room1").content("M2").build();

                when(chatMessageRepository.findByRoomIdOrderByCreatedAtDesc(eq("room1"), any()))
                                .thenReturn(List.of(msg2, msg1));
                when(chatRoomRepository.clearUnreadCount("room1", "e1")).thenReturn(true);

                // when
                CursorPageResponse<ChatMessageResponse> response = chatMessageService
                                .getPreviousMessages("room1", null, null, 20, "e1");

                // then
                assertThat(response.getItems()).hasSize(2);
                assertThat(response.getItems().get(0).getContent()).isEqualTo("M2");
                assertThat(response.getNextCursor()).isNull();
        }
}
