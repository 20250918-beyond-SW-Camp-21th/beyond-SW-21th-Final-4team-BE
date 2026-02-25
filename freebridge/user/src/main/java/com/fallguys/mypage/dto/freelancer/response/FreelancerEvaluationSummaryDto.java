package com.fallguys.mypage.dto.freelancer.response;

public record FreelancerEvaluationSummaryDto(
        Double averageRate, // 전체 평점
        Integer topPercentile, // 상위 %
        Double expertiseRate, // 전문성
        Double communicationRate, // 의사소통
        Double scheduleRate // 일정준수
) {}
