package com.fallguys.mypage.api.web.dto.employer.response;

public record EmployerProjectStatusDto(
        Integer recruitingProjects, // 접수중
        Integer reviewingProjects, // 심사중
        Integer inProgressProjects, // 진행중
        Integer completedProjects // 완료/종결
) {}
