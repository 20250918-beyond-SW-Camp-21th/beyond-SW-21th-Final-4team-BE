package com.fallguys.mypage.api.web.dto.freelancer.response;

import java.util.Map;

public record FreelancerProjectStatusStatsDto(
        Integer appliedProjects,    // 지원한 프로젝트
        Integer inProgressProjects, // 진행중 프로젝트
        Integer completedProjects   // 완료된 프로젝트
) {
    public static FreelancerProjectStatusStatsDto empty() {
        return new FreelancerProjectStatusStatsDto(0, 0, 0);
    }

    /**
     * Redis 역직렬화 결과는 Integer / Long / String 등 다양한 타입일 수 있으므로
     * 와일드카드 맵을 받아 safeInt()로 안전하게 변환합니다.
     */
    public static FreelancerProjectStatusStatsDto from(Map<String, ?> stats) {
        if (stats == null || stats.isEmpty()) {
            return empty();
        }
        return new FreelancerProjectStatusStatsDto(
                safeInt(stats.get("appliedProjects")),
                safeInt(stats.get("inProgressProjects")),
                safeInt(stats.get("completedProjects"))
        );
    }

    /** Number(Integer/Long 등), String, null 을 모두 int로 안전 변환합니다. */
    private static int safeInt(Object value) {
        if (value == null) return 0;
        if (value instanceof Number n) return n.intValue();
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
