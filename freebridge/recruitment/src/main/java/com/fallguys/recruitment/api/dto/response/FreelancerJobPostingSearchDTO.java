package com.fallguys.recruitment.api.dto.response;

import java.util.List;

public record FreelancerJobPostingSearchDTO(
        Long jobPostingId,
        String employerName,
        String title,
        String description,
        List<String> techStack,
        Long budget,
        Integer duration,
        boolean favorite
) {
}
