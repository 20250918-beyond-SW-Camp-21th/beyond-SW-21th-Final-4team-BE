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

    /**
     * Redis에서 받은 리뷰 목록으로 항목별 평균을 계산합니다.
     * JSON 역직렬화 시 정수 값은 Integer/Long으로 들어올 수 있으므로 Object로 수신합니다.
     */
    public static EmployerReviewSummaryResponseDto from(List<Map<String, Object>> reviews) {
        if (reviews == null || reviews.isEmpty()) {
            return empty();
        }

        double sumAtmosphere = 0;
        double sumRequirements = 0;
        double sumSchedule = 0;
        int count = 0;

        for (Map<String, Object> rates : reviews) {
            if (rates == null) continue; // null Map 요소 방어
            sumAtmosphere += safeNumber(rates.get("atmosphereRate"));
            sumRequirements += safeNumber(rates.get("requirementsDetailRate"));
            sumSchedule += safeNumber(rates.get("scheduleAdherenceRate"));
            count++;
        }

        if (count == 0) return empty();

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

    public static EmployerReviewSummaryResponseDto fromAverageMap(Map<String, Object> averages) {
        if (averages == null || averages.isEmpty()) {
            return empty();
        }

        double avgAtmosphere = round1(safeNumber(averages.get("atmosphereRate")));
        double avgRequirements = round1(safeNumber(averages.get("requirementsDetailRate")));
        double avgSchedule = round1(safeNumber(averages.get("scheduleAdherenceRate")));
        double totalAverage = round1((avgAtmosphere + avgRequirements + avgSchedule) / 3.0);

        return new EmployerReviewSummaryResponseDto(
                totalAverage,
                avgAtmosphere,
                avgRequirements,
                avgSchedule
        );
    }

    /** Integer, Long, Double 등 모든 Number 하위 타입과 null을 안전하게 double로 변환합니다. */
    private static double safeNumber(Object value) {
        if (value instanceof Number n) {
            return n.doubleValue();
        }
        return 0.0;
    }

    private static double round1(double value) {
        return Math.round(value * 10.0) / 10.0;
    }
}
