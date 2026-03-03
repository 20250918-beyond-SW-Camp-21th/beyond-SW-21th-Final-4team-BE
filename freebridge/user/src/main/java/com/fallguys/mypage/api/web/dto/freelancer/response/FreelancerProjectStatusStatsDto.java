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

    public static FreelancerProjectStatusStatsDto from(Map<String, Integer> stats) {
        if (stats == null || stats.isEmpty()) {
            return empty();
        }
        return new FreelancerProjectStatusStatsDto(
                java.util.Optional.ofNullable(stats.get("appliedProjects")).orElse(0),
                java.util.Optional.ofNullable(stats.get("inProgressProjects")).orElse(0),
                java.util.Optional.ofNullable(stats.get("completedProjects")).orElse(0)
        );
    }
}
