package com.fallguys.mypage.api.web.dto.freelancer.response;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record FreelancerProfilePreviewResponseDto(
        Long freelancerId,
        Long userId,
        String name,
        String avatarUrl,
        String job,
        String introduction,
        String grade,
        Integer careerYears,
        Long wage,
        List<String> skills,
        LocalDate birthDate,
        String phone,
        String email,
        String address,
        List<FreelancerPreviewEducationDto> educations,
        List<FreelancerPreviewCareerDto> careers,
        List<FreelancerPreviewCertificationDto> certifications,
        String portfolioFileUrl,
        String portfolioFileName,
        LocalDateTime portfolioLastUpdated
) {}
