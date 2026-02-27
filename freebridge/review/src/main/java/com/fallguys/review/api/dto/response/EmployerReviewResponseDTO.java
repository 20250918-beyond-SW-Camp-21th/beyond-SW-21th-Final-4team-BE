package com.fallguys.review.api.dto.response;

import com.fallguys.review.entity.EmployerReview;

import java.time.LocalDateTime;

public record EmployerReviewResponseDTO(
        Long id,
        Long projectId,
        Long employerId,
        Long freelancerId,
        Integer language,
        Integer framework,
        Integer debugging,
        Integer communication,
        Integer schedule,
        Integer dispute,
        String description,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static EmployerReviewResponseDTO from(EmployerReview review) {
        return new EmployerReviewResponseDTO(
                review.getId(),
                review.getProjectId(),
                review.getEmployerId(),
                review.getFreelancerId(),
                review.getLanguage(),
                review.getFramework(),
                review.getDebugging(),
                review.getCommunication(),
                review.getSchedule(),
                review.getDispute(),
                review.getDescription(),
                review.getCreatedAt(),
                review.getUpdatedAt()
        );
    }
}
