package com.fallguys.mypage.api.web.dto.freelancer.response;

/** AI 평판 긍정 지수 응답 DTO */
public record FreelancerAiPositivityIndexDto(
        Double positivityScore,  // AI가 분석한 긍정 지수 (0 ~ 100)
        String grade             // 등급 (예: "EXCELLENT", "GOOD", "AVERAGE", "POOR")
) {}
