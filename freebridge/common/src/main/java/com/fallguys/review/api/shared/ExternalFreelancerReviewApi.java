package com.fallguys.review.api.shared;

import com.fallguys.review.api.shared.response.FreelancerReviewMetricsDto;

public interface ExternalFreelancerReviewApi {
    FreelancerReviewMetricsDto getFreelancerReviewMetrics(Long userId);
}
