package com.fallguys.mypage.service.freelancer;

import com.fallguys.common.ai.port.ReviewEngine;
import com.fallguys.mypage.api.web.dto.freelancer.response.FreelancerAiPositivityIndexDto;
import com.fallguys.common.ai.dto.FreelancerAiReputationReportDto;
import com.fallguys.mypage.api.web.dto.freelancer.response.FreelancerEvaluationSummaryDto;
import com.fallguys.mypage.api.web.dto.freelancer.response.FreelancerStrengthWeaknessDto;
import com.fallguys.mypage.entity.freelancer.Freelancer;
import com.fallguys.mypage.repository.freelancer.FreelancerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.Duration;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class FreelancerReviewService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final FreelancerRepository freelancerRepository;
    private final ReviewEngine reviewEngine;
    private final ObjectMapper objectMapper;

    /**
     * 내 평판/등급 요약 조회
     * Redis Key: freelancer:review:rates:{freelancerId}
     * Expected value: List<Map<String, Double>> { expertiseRate, communicationRate,
     * scheduleRate }
     * topPercentile은 Freelancer 엔티티에서 조회합니다.
     */
    @Transactional(readOnly = true)
    public FreelancerEvaluationSummaryDto getReviewSummary(Long userId) {
        String redisKey = "freelancer:review:rates:" + userId;
        Integer topPercentile = getTopPercentile(userId);

        try {
            Object rawData = redisTemplate.opsForValue().get(redisKey);
            if (rawData == null) {
                return FreelancerEvaluationSummaryDto.empty(topPercentile);
            }
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> reviews = (List<Map<String, Object>>) rawData;
            return FreelancerEvaluationSummaryDto.from(reviews, topPercentile);

        } catch (Exception e) {
            log.error("Failed to parse freelancer review summary from Redis for userId: {}", userId, e);
            return FreelancerEvaluationSummaryDto.empty(topPercentile);
        }
    }

    /**
     * AI 평판 분석 리포트 조회 (Redis 캐싱 적용)
     * Redis Key: freelancer:review:ai_report:{userId}
     * TTL: 24시간
     */
    public FreelancerAiReputationReportDto getAiReputationReport(Long userId) {
        String ratesRedisKey = "freelancer:review:rates:" + userId;
        try {
            Object ratesData = redisTemplate.opsForValue().get(ratesRedisKey);
            // 등록된 리뷰가 명시적으로 비어있을 경우에만 AI 서버 호출 생략
            if (ratesData instanceof List<?> list && list.isEmpty()) {
                return new FreelancerAiReputationReportDto(
                        "아직 충분한 리뷰가 등록되지 않았습니다.",
                        Collections.emptyList(),
                        Collections.emptyList(),
                        Collections.emptyList(),
                        Collections.emptyList()
                );
            }
        } catch (Exception e) {
            log.warn("Failed to check review existence in Redis for userId: {}", userId, e);
        }

        String redisKey = "freelancer:review:ai_report:" + userId;
        try {
            Object cachedData = redisTemplate.opsForValue().get(redisKey);
            if (cachedData != null) {
                // 저장된 캐시가 있을 경우 JSON에서 파싱
                return objectMapper.convertValue(cachedData, FreelancerAiReputationReportDto.class);
            }
        } catch (Exception e) {
            log.warn("Failed to get AI reputation report from Redis for userId: {}", userId, e);
        }

        // 캐시가 없거나, null이어서(ratesData가 아예 없거나) 재생성이 필요한 경우 AI 서버로 호출
        FreelancerAiReputationReportDto report = reviewEngine.getFreelancerAnalysis(userId);

        try {
            if (report != null) {
                redisTemplate.opsForValue().set(redisKey, report, Duration.ofHours(24));
            }
        } catch (Exception e) {
            log.warn("Failed to save AI reputation report to Redis for userId: {}", userId, e);
        }

        return report;
    }

    /**
     * AI 평판 긍정 지수 조회
     */
    public FreelancerAiPositivityIndexDto getAiPositivityIndex(Long userId) {
        FreelancerAiReputationReportDto report = getAiReputationReport(userId);
        if (report == null) {
            return new FreelancerAiPositivityIndexDto(0.0, "POOR");
        }

        double totalScore = 0.0;
        int count = 0;

        if (report.technicalScores() != null) {
            for (com.fallguys.common.ai.dto.FreelancerAiReputationReportDto.ScoreDto score : report.technicalScores()) {
                totalScore += score.score();
                count++;
            }
        }
        if (report.softSkills() != null) {
            for (com.fallguys.common.ai.dto.FreelancerAiReputationReportDto.ScoreDto score : report.softSkills()) {
                totalScore += score.score();
                count++;
            }
        }

        if (count == 0) {
            return new FreelancerAiPositivityIndexDto(0.0, "POOR");
        }

        double posScore = (totalScore / count) * 20.0; // 5점 만점을 100점 만점으로 변환
        String grade = "POOR";
        if (posScore >= 90) {
            grade = "EXCELLENT";
        } else if (posScore >= 70) {
            grade = "GOOD";
        } else if (posScore >= 50) {
            grade = "AVERAGE";
        }

        return new FreelancerAiPositivityIndexDto(posScore, grade);
    }

    /**
     * 프리랜서 강점/약점 분석 조회
     * AI가 분석한 강점 3가지, 약점 3가지를 반환합니다.
     */
    public FreelancerStrengthWeaknessDto getStrengthWeaknessAnalysis(Long userId) {
        FreelancerAiReputationReportDto report = getAiReputationReport(userId);
        if (report == null) {
            return new FreelancerStrengthWeaknessDto(Collections.emptyList(), Collections.emptyList());
        }
        return new FreelancerStrengthWeaknessDto(
                report.strengths() != null ? report.strengths() : Collections.emptyList(),
                report.weaknesses() != null ? report.weaknesses() : Collections.emptyList()
        );
    }

    // ─── 내부 헬퍼 ──────────────────────────────────────────────

    private Integer getTopPercentile(Long userId) {
        try {
            return freelancerRepository.findByUserId(userId)
                    .map(Freelancer::getTopPercentile)
                    .orElse(null);
        } catch (Exception e) {
            log.warn("Failed to get topPercentile for userId: {}", userId, e);
            return null;
        }
    }
}
