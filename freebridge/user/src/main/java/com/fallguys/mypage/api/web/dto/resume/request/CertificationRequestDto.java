package com.fallguys.mypage.api.web.dto.resume.request;

import java.time.LocalDate;

public record CertificationRequestDto(
        String name,
        String issuer,
        LocalDate acquisitionDate
) {}
