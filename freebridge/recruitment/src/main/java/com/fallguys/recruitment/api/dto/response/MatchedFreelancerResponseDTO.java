package com.fallguys.recruitment.api.dto.response;

import com.fallguys.recruitment.entity.ProjectStatus;

import java.time.LocalDateTime;

public record MatchedFreelancerResponseDTO(
        Long projectId,
        Long freelancerId,
        String freelancerName,
        String skills,
        String profileSummary,
        String freelancerStatus,
        ProjectStatus projectStatus,
        LocalDateTime matchedAt
) {
}
