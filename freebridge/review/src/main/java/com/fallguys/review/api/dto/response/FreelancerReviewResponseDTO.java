package com.fallguys.review.api.dto.response;

import com.fallguys.review.entity.FreelancerReview;

import java.time.LocalDateTime;

public record FreelancerReviewResponseDTO(
        Long id,
        Long projectId,
        Long freelancerId,
        Long employerId,
        Integer atmosphere,
        Integer requirementDetail,
        Integer schedule,
        String description,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static FreelancerReviewResponseDTO from(FreelancerReview review) {
        return new FreelancerReviewResponseDTO(
                review.getId(),
                review.getProjectId(),
                review.getFreelancerId(),
                review.getEmployerId(),
                review.getAtmosphere(),
                review.getRequirementDetail(),
                review.getSchedule(),
                review.getDescription(),
                review.getCreatedAt(),
                review.getUpdatedAt()
        );
    }
}
