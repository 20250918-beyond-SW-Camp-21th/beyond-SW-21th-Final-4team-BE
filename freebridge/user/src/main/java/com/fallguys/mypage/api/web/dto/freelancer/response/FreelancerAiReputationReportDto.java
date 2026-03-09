package com.fallguys.mypage.api.web.dto.freelancer.response;

import java.util.List;

/** AI 평판 분석 리포트 응답 DTO */
public record FreelancerAiReputationReportDto(
        String aiSummary,
        List<String> positiveKeywords,
        List<String> negativeKeywords
) {}
