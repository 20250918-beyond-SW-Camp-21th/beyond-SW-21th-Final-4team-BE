package com.fallguys.mypage.api.web.dto.freelancer.response;

public record CrmAlertsDto(
        Boolean rateBumpEligible,
        Boolean burnoutRisk,
        Boolean churnRisk
) {}
