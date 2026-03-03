package com.fallguys.mypage.api.web.dto.freelancer.response;

public record FreelancerProjectStatusStatsDto(
        Integer appliedProjects, // 지원한 프로젝트
        Integer inProgressProjects, // 진행중 프로젝트
        Integer completedProjects // 완료된 프로젝트
) {}
