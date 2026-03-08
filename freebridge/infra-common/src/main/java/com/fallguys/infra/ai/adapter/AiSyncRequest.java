package com.fallguys.infra.ai.adapter;


public record AiSyncRequest(
        Long id,             // 프로젝트 ID
        String type,         // "experience" 고정
        String content,      // 기업 리뷰 내용 (description)
        String status,       // "COMPLETED" 고정
        Long refId,          // 프리랜서 ID (AI 검색용)
        AiReviewScores scores // 상세 평점
) {
    public record AiReviewScores(
            Integer communication,
            Integer debugging,
            Integer framework,
            Integer language,
            Integer schedule
    ) {}
}