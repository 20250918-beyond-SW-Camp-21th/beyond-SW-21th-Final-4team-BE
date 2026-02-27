package com.fallguys.review.api.dto.request;

public record EmployerReviewCreateRequest(
        Long projectId,
        Long freelancerId,
        Integer language,
        Integer framework,
        Integer debugging,
        Integer communication,
        Integer schedule,
        Integer dispute,
        String description
) {
}
