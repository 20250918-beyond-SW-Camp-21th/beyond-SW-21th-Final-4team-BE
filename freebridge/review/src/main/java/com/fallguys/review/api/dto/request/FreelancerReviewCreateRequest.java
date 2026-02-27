package com.fallguys.review.api.dto.request;

public record FreelancerReviewCreateRequest(
        Long projectId,
        Long employerId,
        Integer atmosphere,
        Integer requirementDetail,
        Integer schedule,
        String description
) {
}
