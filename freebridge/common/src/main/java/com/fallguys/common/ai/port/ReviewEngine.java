package com.fallguys.common.ai.port;

import java.util.List;
import java.util.Map;
import com.fallguys.common.ai.dto.FreelancerAiReputationReportDto;

public interface ReviewEngine {
    // 항목별 점수와 텍스트 후기를 분석하여 종합 점수(100점) 및 분석 결과 리턴
    Map<String, Object> analyzeReputation(List<Integer> scores, List<String> reviews);

    FreelancerAiReputationReportDto getFreelancerAnalysis(Long userId);
}
