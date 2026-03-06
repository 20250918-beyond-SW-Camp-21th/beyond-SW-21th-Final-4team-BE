package com.fallguys.chatting.api.web.dto.request;

import com.fallguys.chatting.domain.MessageType;
import lombok.Getter;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.Map;

@Getter
@NoArgsConstructor
public class ChatMessageRequest {
    @NotBlank(message = "채팅방 ID는 필수입니다.")
    private String roomId;

    @NotBlank(message = "발송자 ID는 필수입니다.")
    private String senderId;

    @NotBlank(message = "메시지 내용은 필수입니다.")
    private String content;

    @NotNull(message = "메시지 타입은 필수입니다.")
    private MessageType type;

    // 부가 정보 (예: System 메시지이거나 Contract Alert일 경우 사용)
    private Map<String, Object> metadata;
}
