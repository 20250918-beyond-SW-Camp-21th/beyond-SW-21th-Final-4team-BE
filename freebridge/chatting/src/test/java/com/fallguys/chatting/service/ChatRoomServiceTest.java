package com.fallguys.chatting.service;

import com.fallguys.chatting.domain.ChatRoom;
import com.fallguys.chatting.api.web.dto.request.ChatRoomCreateRequest;
import com.fallguys.chatting.api.web.dto.response.ChatRoomResponse;
import com.fallguys.chatting.repository.ChatRoomRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatRoomServiceTest {

    @Mock
    private ChatRoomRepository chatRoomRepository;

    @InjectMocks
    private ChatRoomService chatRoomService; // 구현 전이므로 이후 컴파일 에러 발생 예상

    @Test
    @DisplayName("채팅방을 성공적으로 생성한다")
    void createChatRoom_Success() {
        // given
        ChatRoomCreateRequest request = new ChatRoomCreateRequest();
        // 실제로는 ReflectionTestUtils나 protected setter/constructor를 사용하여 필드 주입
        // 이 예시에서는 리플렉션으로 가정하거나 DTO에 @Builder나 생성자가 필요함.
        // 임시로 Mocking 시뮬레이션

        ChatRoom savedRoom = ChatRoom.builder()
                .id("room1")
                .participants(List.of("e1", "f1"))
                .participantNames(Map.of("e1", "Employer A", "f1", "Freelancer B"))
                .relatedJobId("job1")
                .build();

        when(chatRoomRepository.save(any(ChatRoom.class))).thenReturn(savedRoom);

        // when
        ChatRoomResponse response = chatRoomService.createChatRoom(List.of("e1", "f1"),
                Map.of("e1", "Employer A", "f1", "Freelancer B"), "job1", null, null);

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
