package com.fallguys.chatting.api.web.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ChatRoomContractUpdateRequest {

    private Long contractId;
    private Boolean overrideExisting;
}
