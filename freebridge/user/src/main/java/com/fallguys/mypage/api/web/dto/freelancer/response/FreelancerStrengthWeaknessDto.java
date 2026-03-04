package com.fallguys.mypage.api.web.dto.freelancer.response;

import java.util.List;

/**
 * AI 분석 기반 프리랜서 강점/약점 분석 응답 DTO
 * - strengths: 강점 항목 (최대 3가지)
 * - weaknesses: 약점 항목 (최대 3가지)
 */
public record FreelancerStrengthWeaknessDto(
        List<String> strengths,
        List<String> weaknesses
) {}
