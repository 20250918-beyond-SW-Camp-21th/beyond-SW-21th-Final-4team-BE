package com.fallguys.recruitment.api.dto.request;
import java.util.List;

public record JobPostingCreateDTO(
        String title,
        String description,
        List<String> techStack,
        Long budget,
        Integer duration,
        Integer headcount
) {}
