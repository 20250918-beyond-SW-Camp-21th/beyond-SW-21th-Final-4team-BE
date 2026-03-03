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
     * 각 Map: { "expertiseRate": 4.5, "communicationRate": 5.0, "scheduleRate": 3.5 }
     * Map value가 null인 경우 0.0으로 처리합니다.
     */
    public static FreelancerEvaluationSummaryDto from(List<Map<String, Double>> reviews, Integer topPercentile) {
        if (reviews == null || reviews.isEmpty()) {
            return empty(topPercentile);
        }
        double sumExpertise = 0;
        double sumCommunication = 0;
        double sumSchedule = 0;
        int count = 0;

        for (Map<String, Double> rates : reviews) {
            if (rates == null) continue; // null Map 요소 방어
            // getOrDefault는 키가 있고 값이 null이면 null을 반환하므로 별도 null 처리
            sumExpertise     += safeDouble(rates.get("expertiseRate"));
            sumCommunication += safeDouble(rates.get("communicationRate"));
            sumSchedule      += safeDouble(rates.get("scheduleRate"));
            count++;
        }

        if (count == 0) return empty(topPercentile);

        double avgExpertise     = round1(sumExpertise     / count);
        double avgCommunication = round1(sumCommunication / count);
        double avgSchedule      = round1(sumSchedule      / count);
        double totalAverage     = round1((avgExpertise + avgCommunication + avgSchedule) / 3.0);

        return new FreelancerEvaluationSummaryDto(totalAverage, topPercentile, avgExpertise, avgCommunication, avgSchedule);
    }

    private static double safeDouble(Double value) {
        return value != null ? value : 0.0;
    }

    private static double round1(double value) {
        return Math.round(value * 10.0) / 10.0;
    }
}
