package com.fallguys.recruitment.api.dto.response;

import com.fallguys.recruitment.entity.ProjectStatus;

import java.time.LocalDate;

public record EmployerProjectSearchDTO(
        Long projectId,
        Long jobPostingId,
        Long freelancerId,
        String projectName,
        Integer headcount,
        LocalDate startDate,
        LocalDate endDate,
        ProjectStatus status
) {
}
