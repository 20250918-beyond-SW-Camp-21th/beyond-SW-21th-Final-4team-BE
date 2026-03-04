package com.fallguys.mypage.api.web.dto.employer.response;

import java.util.List;

// 고용주 Ai 평판 Dto
public record EmployerReputationAiResponseDto(
        String aiSummary,
        List<String> positiveKeywords,
        List<String> negativeKeywords
) {}
