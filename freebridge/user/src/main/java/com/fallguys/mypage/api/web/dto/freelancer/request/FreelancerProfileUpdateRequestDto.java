package com.fallguys.mypage.api.web.dto.freelancer.request;

import java.util.List;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record FreelancerProfileUpdateRequestDto(
        @NotBlank(message = "직무(job)를 입력해주세요.")
        @Size(max = 50, message = "직무 이름은 50자 이내로 작성해주세요.")
        String job,

        @NotBlank(message = "자기소개(introduction)를 입력해주세요.")
        @Size(max = 1000, message = "자기소개는 1000자 이내로 작성해주세요.")
        String introduction,

        @NotNull(message = "경력 연수(careerYears)를 입력해주세요.")
        @Min(value = 0, message = "경력 연수는 0 이상이어야 합니다.")
        Integer careerYears,

        @NotNull(message = "희망 시급(wage)을 입력해주세요.")
        @Min(value = 0, message = "희망 시급은 0 이상이어야 합니다.")
        Long wage,

        @NotNull(message = "기술 스택(skills) 목록을 전달해야 합니다.")
        @NotEmpty(message = "기술 스택을 최소 1개 이상 입력해주세요.")
        List<@NotBlank(message = "기술 스택 항목은 빈칸일 수 없습니다.") String> skills
) {}
