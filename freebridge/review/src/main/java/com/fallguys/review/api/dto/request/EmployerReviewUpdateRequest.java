package com.fallguys.review.api.dto.request;

public record EmployerReviewUpdateRequest(
        Integer language,
        Integer framework,
        Integer debugging,
        Integer communication,
        Integer schedule,
        Integer dispute,
        String description
) {
}
