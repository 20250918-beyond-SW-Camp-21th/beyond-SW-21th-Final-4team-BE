package com.fallguys.mypage.dto.employer.response;

public record EmployerApplicantStatusResponseDto(
        Long freelancerId,
        String freelancerName,
        String job,
        String grade,
        String applyStatus // 검토중, 면접, 합격 등
) {}
