package com.fallguys.mypage.dto.response.employer;

public record EmployerRatingDto(
        Double averageRate, // 전체평점
        Double atmosphereRate, // 사내분위기
        Double salarySatisfactionRate, // 급여만족도
        Double scheduleAdherenceRate // 일정준수
) {}
