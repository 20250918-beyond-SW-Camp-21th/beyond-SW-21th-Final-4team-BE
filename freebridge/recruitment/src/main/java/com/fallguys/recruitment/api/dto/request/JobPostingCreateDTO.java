package com.fallguys.recruitment.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record JobPostingCreateDTO(
        String title,
        String description,
        List<String> techStack,
        Long budget,
        @NotNull
        @Min(1)
        @Schema(description = "예상 프로젝트 기간(개월 수)", example = "3")
        Integer duration,
        Integer headcount
) {}
