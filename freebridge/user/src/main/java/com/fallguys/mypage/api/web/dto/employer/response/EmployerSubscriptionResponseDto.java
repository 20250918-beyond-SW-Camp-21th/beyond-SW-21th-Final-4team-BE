package com.fallguys.mypage.api.web.dto.employer.response;

import java.time.LocalDateTime;
import java.util.List;

// 고용주 구독 정보 Response
public record EmployerSubscriptionResponseDto(
        String currentPlan, // BASIC, PRIME 등
        List<String> features,
        LocalDateTime nextBillingDate
) {}
