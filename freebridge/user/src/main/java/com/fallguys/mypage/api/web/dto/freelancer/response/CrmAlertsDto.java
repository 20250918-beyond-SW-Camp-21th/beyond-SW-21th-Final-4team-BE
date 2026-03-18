package com.fallguys.mypage.api.web.dto.freelancer.response;

public record CrmAlertsDto(
        Boolean isOnboardingNeeded,
        Boolean isApplyEncouraged,
        Boolean isPortfolioImproveNeeded,
        Boolean isRateBumpEligible,
        Boolean isBurnoutWarning,
        Boolean isChurnWarning
) {}
