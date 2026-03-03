package com.fallguys.mypage.api.web.dto.employer.response;

public record EmployerApplicantStatusResponseDto(
        Long freelancerId,      // 특정 프로젝트 참여 중인 프리랜서(projectId로 찾을 예정)
        String applyStatus      // 검토중, 면접, 합격
) {}
