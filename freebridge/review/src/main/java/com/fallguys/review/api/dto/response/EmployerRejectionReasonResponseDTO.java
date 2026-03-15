package com.fallguys.review.api.dto.response;

import java.time.LocalDateTime;

public record EmployerRejectionReasonResponseDTO(
        Long id,
        Long projectId,
        String projectTitle,
        Long employerId,
        Long freelancerId,
        String reason,
        LocalDateTime createdAt
) {
}
