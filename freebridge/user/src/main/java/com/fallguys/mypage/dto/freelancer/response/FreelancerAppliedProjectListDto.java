package com.fallguys.mypage.dto.freelancer.response;

public record FreelancerAppliedProjectListDto(
        Long projectId,
        String title,
        String employerName,
        String applyStatus, // 심사중, 합격, 거절
        Long appliedAt // 지원일자 Timestamp 혹은 LocalDateTime
) {}
