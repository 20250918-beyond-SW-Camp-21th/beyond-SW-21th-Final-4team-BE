package com.fallguys.mypage.dto.employer.response;

import java.time.LocalDateTime;

public record EmployerProjectListResponseDto(
        Long projectId,
        String title,
        String status, // 대기중, 모집중, 진행중 등
        Integer applicantCount,
        LocalDateTime createdAt,
        LocalDateTime deadline
) {}
