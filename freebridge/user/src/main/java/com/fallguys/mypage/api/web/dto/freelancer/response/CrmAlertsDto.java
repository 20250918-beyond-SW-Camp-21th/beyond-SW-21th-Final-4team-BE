package com.fallguys.mypage.api.web.dto.freelancer.response;

public record CrmAlertsDto(
        boolean rateBumpEligible,
        boolean burnoutRisk,
        boolean churnRisk
) {}
