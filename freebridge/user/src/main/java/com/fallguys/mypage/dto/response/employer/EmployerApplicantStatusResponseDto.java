package com.fallguys.mypage.dto.response.employer;

public record EmployerApplicantStatusResponseDto(
        Long freelancerId,
        String freelancerName,
        String job,
        String grade,
        String applyStatus // 검토중, 면접, 합격 등
) {}
