package com.fallguys.chatting.service;

import com.fallguys.chatting.domain.ChatRoom;
import com.fallguys.chatting.api.web.dto.response.ChatRoomResponse;
import com.fallguys.chatting.repository.ChatRoomRepository;
import com.fallguys.chatting.repository.UnreadMessageRedisRepository;
import com.fallguys.common.api.contract.ContractQuery;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatRoomServiceTest {

    @Mock
    private ChatRoomRepository chatRoomRepository;

    @Mock
    private ChatPresenceService chatPresenceService;

    @Mock
    private ChatMessageService chatMessageService;

    @Mock
    private ContractQuery contractQuery;

    @Mock
    private UnreadMessageRedisRepository unreadMessageRedisRepository;

    @InjectMocks
    private ChatRoomService chatRoomService;

    @Test
    @DisplayName("채팅방을 성공적으로 생성한다")
    void createChatRoom_Success() {
        // given
        ChatRoom savedRoom = ChatRoom.builder()
                .id("room1")
                .participants(List.of("e1", "f1"))
                .participantNames(Map.of("e1", "Employer A", "f1", "Freelancer B"))
                .relatedJobId("job1")
                .build();

        when(chatRoomRepository.findRoomByContractAndParticipants(null, List.of("e1", "f1"))).thenReturn(Optional.empty());
        when(chatRoomRepository.save(any(ChatRoom.class))).thenReturn(savedRoom);

        // when
        ChatRoomResponse response = chatRoomService.createChatRoom(List.of("e1", "f1"),
                Map.of("e1", "Employer A", "f1", "Freelancer B"), "job1", null, null, null);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getRoomId()).isEqualTo("room1");
        assertThat(response.getParticipants()).containsExactly("e1", "f1");
    }

    @Test
    @DisplayName("내가 속한 채팅방 목록을 모두 조회한다")
    void getChatRooms_Success() {
        ChatRoom room1 = ChatRoom.builder().id("room1").participants(List.of("e1", "f1")).build();
        ChatRoom room2 = ChatRoom.builder().id("room2").participants(List.of("e1", "f2")).build();

        when(chatRoomRepository.findActiveRoomsByParticipant("e1")).thenReturn(List.of(room1, room2));

        List<ChatRoomResponse> rooms = chatRoomService.getChatRoomsByParticipant("e1");

        assertThat(rooms).hasSize(2);
        assertThat(rooms.get(0).getRoomId()).isEqualTo("room1");
        assertThat(rooms.get(1).getRoomId()).isEqualTo("room2");
    }
}
