package com.fallguys.matchs.api.dto.request;

public record ApplicationCreateRequest(
        Long jobPostingId,
        String message
) {
}
