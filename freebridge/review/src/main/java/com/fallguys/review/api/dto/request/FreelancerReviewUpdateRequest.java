package com.fallguys.review.api.dto.request;

public record FreelancerReviewUpdateRequest(
        Integer atmosphere,
        Integer requirementDetail,
        Integer schedule,
        String description
) {
}
