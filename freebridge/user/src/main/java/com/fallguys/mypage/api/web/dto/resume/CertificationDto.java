package com.fallguys.mypage.api.web.dto.resume;

public record CertificationDto(
        Long certificationId,
        String certificationName,
        String issueOrganization,
        String acquisitionDate
) {}
