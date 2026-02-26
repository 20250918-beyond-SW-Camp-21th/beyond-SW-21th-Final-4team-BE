package com.fallguys.matchs.api.dto.response;

import com.fallguys.matchs.entity.MatchsStatus;

import java.time.LocalDateTime;

public record ApplicationResponseDTO(
        Long applicationId,
        Long jobPostingId,
        Long freelancerId,
        Long employerId,
        String message,
        MatchsStatus status,
        LocalDateTime createdAt
) {
}
