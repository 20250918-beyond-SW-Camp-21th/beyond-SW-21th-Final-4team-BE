package com.fallguys.recruitment.api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public record FreelancerJobPostingSearchDTO(
        Long jobPostingId,
        String employerName,
        String title,
        String description,
        List<String> techStack,
        Long budget,
        @Schema(description = "예상 프로젝트 기간(개월 수)", example = "3")
        Integer duration,
        Integer headcount,
        Integer matchedHeadcount,
        boolean favorite
) {
}
