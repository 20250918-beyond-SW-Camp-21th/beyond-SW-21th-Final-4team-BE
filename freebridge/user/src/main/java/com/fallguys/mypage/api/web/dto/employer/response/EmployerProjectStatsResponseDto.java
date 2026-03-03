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

    public static EmployerProjectStatsResponseDto from(Map<String, Integer> stats) {
        if (stats == null || stats.isEmpty()) {
            return empty();
        }
        return new EmployerProjectStatsResponseDto(
                java.util.Optional.ofNullable(stats.get("totalProjects")).orElse(0),
                java.util.Optional.ofNullable(stats.get("activeApplicants")).orElse(0),
                java.util.Optional.ofNullable(stats.get("contractedFreelancers")).orElse(0)
        );
    }
}