package com.fallguys.mypage.api.web.dto.freelancer.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 프리랜서 등급 저장 요청 DTO
 * type: education | certification
 */
public record GradeSaveRequestDto(
        @NotBlank(message = "type은 필수입니다. (education | certification)")
        String type,

        // type=education 일 때만 사용 (예: 전문학사/학사/석사/박사)
        String education,

        // type=certification 일 때만 사용 (예: 산업기사/기사/기술사/기능사)
        String certification,

        @NotNull(message = "yearsOfExperience는 필수입니다.")
        @Min(value = 0, message = "yearsOfExperience는 0 이상이어야 합니다.")
        Integer yearsOfExperience,

        @NotBlank(message = "grade는 필수입니다. (초급/중급/고급/특급)")
        String grade
) {}
