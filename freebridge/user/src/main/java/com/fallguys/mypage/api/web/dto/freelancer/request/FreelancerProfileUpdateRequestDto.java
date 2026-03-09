package com.fallguys.mypage.api.web.dto.freelancer.request;

import java.time.LocalDate;
import java.util.List;

import jakarta.validation.constraints.Max;
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

        @NotNull(message = "희망 단가(wage)를 입력해주세요.")
        @Min(value = 0, message = "희망 단가는 0 이상이어야 합니다.")
        Long wage,

        @NotNull(message = "기술 스택(skills) 목록은 필수입니다.")
        @NotEmpty(message = "기술 스택은 최소 1개 이상 입력해주세요.")
        List<@NotBlank(message = "기술 스택 항목은 빈 값일 수 없습니다.") String> skills,

        @Size(max = 50, message = "근무 형태(workType)는 50자 이내로 작성해주세요.")
        String workType,

        LocalDate availableStartDate,

        @Size(max = 50, message = "근무 방식(workStyle)은 50자 이내로 작성해주세요.")
        String workStyle,

        @Size(max = 100, message = "근무 위치(workLocation)는 100자 이내로 작성해주세요.")
        String workLocation,

        @Size(max = 50, message = "이름은 50자 이내로 작성해주세요.")
        String name,

        @Min(value = 0, message = "전문성 점수는 0 이상이어야 합니다.")
        @Max(value = 5, message = "전문성 점수는 5 이하여야 합니다.")
        Integer expertiseProgramming,

        @Min(value = 0, message = "프레임워크 점수는 0 이상이어야 합니다.")
        @Max(value = 5, message = "프레임워크 점수는 5 이하여야 합니다.")
        Integer expertiseFramework,

        @Min(value = 0, message = "문제해결 점수는 0 이상이어야 합니다.")
        @Max(value = 5, message = "문제해결 점수는 5 이하여야 합니다.")
        Integer expertiseProblemSolving,

        @Min(value = 0, message = "소통 점수는 0 이상이어야 합니다.")
        @Max(value = 5, message = "소통 점수는 5 이하여야 합니다.")
        Integer collaborationCommunication,

        @Min(value = 0, message = "일정 준수 점수는 0 이상이어야 합니다.")
        @Max(value = 5, message = "일정 준수 점수는 5 이하여야 합니다.")
        Integer collaborationScheduleAdherence,

        @Min(value = 0, message = "분쟁 대응 점수는 0 이상이어야 합니다.")
        @Max(value = 5, message = "분쟁 대응 점수는 5 이하여야 합니다.")
        Integer collaborationDispute,

        @Min(value = 0, message = "평점은 0 이상이어야 합니다.")
        @Max(value = 5, message = "평점은 5 이하여야 합니다.")
        Double averageRating
) {}
