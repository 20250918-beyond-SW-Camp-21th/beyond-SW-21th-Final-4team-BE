package com.fallguys.mypage.dto.resume;

public record CertificationDto(
        Long certificationId,
        String certificationName,
        String issueOrganization,
        String acquisitionDate
) {}
