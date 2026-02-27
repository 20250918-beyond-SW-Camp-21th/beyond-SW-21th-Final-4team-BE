package com.fallguys.mypage.api.web.dto.employer.response;

public record EmployerProjectStatsResponseDto(
        Integer totalProjects, //총 프로젝트 수
        Integer activeApplicants, //현재 고용주와 작업중인 프리랜서 수
        Integer contractedFreelancers //계약완료한 모든 프리랜서
) {}