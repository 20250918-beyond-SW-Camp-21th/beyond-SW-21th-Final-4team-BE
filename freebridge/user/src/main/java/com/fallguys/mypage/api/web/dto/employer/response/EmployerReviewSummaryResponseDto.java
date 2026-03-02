package com.fallguys.mypage.api.web.dto.employer.response;

import java.util.List;
import java.util.Map;

// 고용주 평판 정리 Dto
public record EmployerReviewSummaryResponseDto(
        Double averageRate,
        Double atmosphereRate,
        Double requirementsDetailRate,
        Double scheduleAdherenceRate
) {
    public static EmployerReviewSummaryResponseDto empty() {
        return new EmployerReviewSummaryResponseDto(0.0, 0.0, 0.0, 0.0);
    }

    public static EmployerReviewSummaryResponseDto from(List<Map<String, Double>> reviews) {
        if (reviews == null || reviews.isEmpty()) {
            return empty();
        }

        double sumAtmosphere = 0;
        double sumRequirements = 0;
        double sumSchedule = 0;

        for (Map<String, Double> rates : reviews) {
            sumAtmosphere += rates.getOrDefault("atmosphereRate", 0.0);
            sumRequirements += rates.getOrDefault("requirementsDetailRate", 0.0);
            sumSchedule += rates.getOrDefault("scheduleAdherenceRate", 0.0);
        }

        int count = reviews.size();
        double avgAtmosphere = Math.round((sumAtmosphere / count) * 10.0) / 10.0;
        double avgRequirements = Math.round((sumRequirements / count) * 10.0) / 10.0;
        double avgSchedule = Math.round((sumSchedule / count) * 10.0) / 10.0;
        
        double totalAverage = Math.round(((avgAtmosphere + avgRequirements + avgSchedule) / 3.0) * 10.0) / 10.0;

        return new EmployerReviewSummaryResponseDto(
                totalAverage,
                avgAtmosphere,
                avgRequirements,
                avgSchedule
        );
    }
}
