package com.fallguys.mypage.dto.employer.response;

import java.time.LocalDateTime;

// 고용주 리뷰 리스트 Dto
public record EmployerReviewListResponseDto(
        Long reviewId,
        String freelancerName,
        String content,
        Double rate,
        LocalDateTime createdAt
) {}
