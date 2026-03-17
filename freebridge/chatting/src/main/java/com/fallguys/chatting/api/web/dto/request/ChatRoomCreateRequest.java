package com.fallguys.chatting.api.web.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;
import java.util.Map;

@Getter
@NoArgsConstructor
public class ChatRoomCreateRequest {

    @NotEmpty(message = "참여자는 최소 한 명 이상이어야 합니다.")
    private List<String> participants;

    // 프론트엔드에서 캐싱할 이름 맵
    private Map<String, String> participantNames;

    // 해당 채팅방을 파생시킨 원본 정보
    private String relatedJobId;
    private String relatedApplicationId;
    private String relatedProposalId;
    private Long contractId;
}
