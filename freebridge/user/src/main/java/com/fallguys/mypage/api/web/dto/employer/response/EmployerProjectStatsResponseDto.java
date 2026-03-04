package com.fallguys.mypage.api.web.dto.employer.response;

import java.util.Map;

public record EmployerProjectStatsResponseDto(
        Integer totalProjects, //총 프로젝트 수
        Integer activeApplicants, //현재 고용주와 작업중인 프리랜서 수
        Integer contractedFreelancers //계약완료한 모든 프리랜서
) {
    public static EmployerProjectStatsResponseDto empty() {
        return new EmployerProjectStatsResponseDto(0, 0, 0);
    }

    /**
     * Redis 역직렬화 결과는 Integer / Long / String 등 다양한 타입일 수 있으므로
     * 와일드카드 맵을 받아 safeInt()로 안전하게 변환합니다.
     */
    public static EmployerProjectStatsResponseDto from(Map<String, ?> stats) {
        if (stats == null || stats.isEmpty()) {
            return empty();
        }
        return new EmployerProjectStatsResponseDto(
                safeInt(stats.get("totalProjects")),
                safeInt(stats.get("activeApplicants")),
                safeInt(stats.get("contractedFreelancers"))
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