package com.fallguys.mypage.api.web.dto.resume.request;

import java.time.LocalDate;
import java.util.List;

public record ResumeRequestDto(
        String name,
        LocalDate birthDate,
        String phone,
        String email,
        String address,
        List<EducationRequestDto> educations,
        List<CareerRequestDto> careers,
        List<CertificationRequestDto> certifications
) {}
