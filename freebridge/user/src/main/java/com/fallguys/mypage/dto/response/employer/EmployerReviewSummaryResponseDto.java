package com.fallguys.mypage.dto.response.employer;

// 고용주 평판 정리 Dto
public record EmployerReviewSummaryResponseDto(
        Double averageRate,
        Double atmosphereRate,
        Double requirementsDetailRate,
        Double scheduleAdherenceRate
) {}
