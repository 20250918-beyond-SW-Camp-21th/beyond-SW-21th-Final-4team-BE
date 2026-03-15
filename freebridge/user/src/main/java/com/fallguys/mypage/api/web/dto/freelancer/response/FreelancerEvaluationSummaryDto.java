package com.fallguys.mypage.api.web.dto.freelancer.response;

import java.util.List;
import java.util.Map;

public record FreelancerEvaluationSummaryDto(
        Double averageRate,       // 전체 평점 (항목 평균)
        Integer topPercentile,    // 상위 % (Freelancer 엔티티 필드)
        Double expertiseRate,     // 전문성
        Double communicationRate, // 의사소통
        Double scheduleRate       // 일정준수
) {
    public static FreelancerEvaluationSummaryDto empty(Integer topPercentile) {
        return new FreelancerEvaluationSummaryDto(0.0, topPercentile, 0.0, 0.0, 0.0);
    }

    /**
     * Redis에서 받은 리뷰 목록으로 항목별 평균을 계산합니다.
     * 각 Map: { "expertiseRate": 4.5, "communicationRate": 5, "scheduleRate": 3.5 }
     * JSON 역직렬화 시 정수 값은 Integer/Long으로 들어올 수 있으므로 Object로 수신합니다.
     */
    public static FreelancerEvaluationSummaryDto from(List<Map<String, Object>> reviews, Integer topPercentile) {
        if (reviews == null || reviews.isEmpty()) {
            return empty(topPercentile);
        }
        double sumExpertise = 0;
        double sumCommunication = 0;
        double sumSchedule = 0;
        int count = 0;

        for (Map<String, Object> rates : reviews) {
            if (rates == null) continue; // null Map 요소 방어
            sumExpertise     += safeNumber(rates.get("expertiseRate"));
            sumCommunication += safeNumber(rates.get("communicationRate"));
            sumSchedule      += safeNumber(rates.get("scheduleRate"));
            count++;
        }

        if (count == 0) return empty(topPercentile);

        double avgExpertise     = round1(sumExpertise     / count);
        double avgCommunication = round1(sumCommunication / count);
        double avgSchedule      = round1(sumSchedule      / count);
        double totalAverage     = round1((avgExpertise + avgCommunication + avgSchedule) / 3.0);

        return new FreelancerEvaluationSummaryDto(totalAverage, topPercentile, avgExpertise, avgCommunication, avgSchedule);
    }

    public static FreelancerEvaluationSummaryDto fromAverageMap(Map<String, Object> averages, Integer topPercentile) {
        if (averages == null || averages.isEmpty()) {
            return empty(topPercentile);
        }

        double avgProgramming = safeNumber(averages.get("programming"));
        double avgFramework = safeNumber(averages.get("framework"));
        double avgDebugging = safeNumber(averages.get("debugging"));
        double avgCommunication = safeNumber(averages.get("communication"));
        double avgSchedule = safeNumber(averages.get("schedule"));

        double expertiseRate = round1((avgProgramming + avgFramework + avgDebugging) / 3.0);
        double communicationRate = round1(avgCommunication);
        double scheduleRate = round1(avgSchedule);
        double totalAverage = round1((expertiseRate + communicationRate + scheduleRate) / 3.0);

        return new FreelancerEvaluationSummaryDto(
                totalAverage,
                topPercentile,
                expertiseRate,
                communicationRate,
                scheduleRate
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
