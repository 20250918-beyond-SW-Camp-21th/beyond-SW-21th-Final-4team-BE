package com.fallguys.recruitment.api.dto.response;

import com.fallguys.recruitment.entity.JobPostingStatus;

import java.util.List;

public record JobPostingSearchDTO(
        Long jobPostingId,
        String employerName,
        String title,
        String description,
        List<String> techStack,
        Long budget,
        Integer duration,
        JobPostingStatus status
) {}
