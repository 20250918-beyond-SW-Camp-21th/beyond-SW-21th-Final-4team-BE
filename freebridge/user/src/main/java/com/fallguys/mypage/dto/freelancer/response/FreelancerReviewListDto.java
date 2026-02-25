package com.fallguys.mypage.dto.freelancer.response;

import java.time.LocalDateTime;

public record FreelancerReviewListDto(
        Long reviewId,
        String projectName,
        String employerName,
        Double rate,
        String content,
        LocalDateTime createdAt
) {}
