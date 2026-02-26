package com.fallguys.recruitment.api.dto.request;

import com.fallguys.recruitment.entity.JobPostingStatus;
import java.util.List;

public record JobPostingUpdateDTO(
        String title,
        String description,
        List<String> techStack,
        Long budget,
        Integer duration,
        Integer headcount,
        JobPostingStatus status
) {}
