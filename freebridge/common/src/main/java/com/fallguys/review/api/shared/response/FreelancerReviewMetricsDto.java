package com.fallguys.review.api.shared.response;

public record FreelancerReviewMetricsDto(
        Double programming,
        Double framework,
        Double debugging,
        Double communication,
        Double schedule,
        Double dispute,
        Double averageRate
) {
    public static FreelancerReviewMetricsDto empty() {
        return new FreelancerReviewMetricsDto(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0);
    }
}
