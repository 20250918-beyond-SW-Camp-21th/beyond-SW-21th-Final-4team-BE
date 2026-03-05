package com.fallguys.chatting.api.web.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * 커서 기반 페이징(Cursor-based Pagination) 공통 응답 DTO
 */
@Getter
@Builder
public class CursorPageResponse<T> {
    private List<T> items;
    private String nextCursor; // 마지막 요소의 식별자(ID) 또는 생성시간
    private boolean hasNext; // 다음 페이지 존재 여부
}
