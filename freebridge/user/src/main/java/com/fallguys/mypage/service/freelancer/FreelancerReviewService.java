package com.fallguys.mypage.service.freelancer;

import com.fallguys.common.ai.port.ReviewEngine;
import com.fallguys.common.exception.BusinessException;
import com.fallguys.common.exception.ErrorCode;
import com.fallguys.mypage.api.web.dto.freelancer.response.FreelancerAiPositivityIndexDto;
import com.fallguys.common.ai.dto.FreelancerAiReputationReportDto;
import com.fallguys.mypage.api.web.dto.freelancer.response.FreelancerEvaluationSummaryDto;
import com.fallguys.mypage.api.web.dto.freelancer.response.FreelancerStrengthWeaknessDto;
import com.fallguys.mypage.entity.freelancer.Collaboration;
import com.fallguys.mypage.entity.freelancer.Expertise;
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
    private static final String REVIEW_RATES_KEY_PREFIX = "freelancer:review:rates:";
    private static final String REVIEW_RATE_KEY_PREFIX = "freelancer:review:rate:";
    private static final String REVIEW_AI_REPORT_KEY_PREFIX = "freelancer:review:ai_report:";

    private final RedisTemplate<String, Object> redisTemplate;
    private final FreelancerRepository freelancerRepository;
    private final ReviewEngine reviewEngine;
    private final ObjectMapper objectMapper;

    /**
     * 내 평판/등급 요약 조회
     * Redis Key: freelancer:review:rates:{freelancerId}
     * Expected value: List<Map<String, Double>> { expertiseRate, communicationRate, scheduleRate }
     * topPercentile은 Freelancer 엔티티에서 조회합니다.
     */
    @Transactional(readOnly = true)
    public FreelancerEvaluationSummaryDto getReviewSummary(Long userId) {
        Integer topPercentile = getTopPercentile(userId);

        try {
            List<Map<String, Object>> reviews = readRawReviewRates(userId);
            if (reviews.isEmpty()) {
                return FreelancerEvaluationSummaryDto.empty(topPercentile);
            }
            return FreelancerEvaluationSummaryDto.from(reviews, topPercentile);

        } catch (Exception e) {
            log.error("Failed to parse freelancer review summary from Redis for userId: {}", userId, e);
            return FreelancerEvaluationSummaryDto.empty(topPercentile);
        }
    }

    @Transactional
    public void syncReviewScoresToFreelancer(Long userId) {
        List<Map<String, Object>> reviews = readRawReviewRates(userId);
        if (reviews.isEmpty()) {
            log.info("No freelancer review rates found in Redis. Skip expertise/collaboration sync. userId={}", userId);
            return;
        }

        Freelancer freelancer = freelancerRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        ReviewScoreSnapshot snapshot = ReviewScoreSnapshot.from(reviews);

        freelancer.updateExpertise(new Expertise(
                toStoredScore(snapshot.programming()),
                toStoredScore(snapshot.framework()),
                toStoredScore(snapshot.debugging())
        ));

        freelancer.updateCollaboration(new Collaboration(
                toStoredScore(snapshot.communication()),
                toStoredScore(snapshot.schedule()),
                toStoredScore(snapshot.dispute())
        ));

        freelancer.updateAverageRate(snapshot.totalScore());
    }

    /**
     * AI 평판 분석 리포트 조회 (Redis 캐싱 적용)
     * Redis Key: freelancer:review:ai_report:{userId}
     * TTL: 24시간
     */
    public FreelancerAiReputationReportDto getAiReputationReport(Long userId) {
        String ratesRedisKey = REVIEW_RATES_KEY_PREFIX + userId;
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

        String redisKey = REVIEW_AI_REPORT_KEY_PREFIX + userId;
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

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> readRawReviewRates(Long userId) {
        Object rawData = redisTemplate.opsForValue().get(REVIEW_RATE_KEY_PREFIX + userId);
        if (!(rawData instanceof List<?> rawList) || rawList.isEmpty()) {
            rawData = redisTemplate.opsForValue().get(REVIEW_RATES_KEY_PREFIX + userId);
        }
        if (!(rawData instanceof List<?> rawList) || rawList.isEmpty()) {
            return Collections.emptyList();
        }
        return (List<Map<String, Object>>) rawList;
    }

    private Integer toStoredScore(Double score) {
        if (score == null) {
            return 0;
        }
        return (int) Math.round(Math.max(0.0, Math.min(5.0, score)));
    }

    private record ReviewScoreSnapshot(
            Double programming,
            Double framework,
            Double debugging,
            Double communication,
            Double schedule,
            Double dispute
    ) {
        private static ReviewScoreSnapshot from(List<Map<String, Object>> reviews) {
            double sumProgramming = 0.0;
            double sumFramework = 0.0;
            double sumDebugging = 0.0;
            double sumCommunication = 0.0;
            double sumSchedule = 0.0;
            double sumDispute = 0.0;

            for (Map<String, Object> review : reviews) {
                sumProgramming += safeNumber(review.get("programming"));
                sumFramework += safeNumber(review.get("framework"));
                sumDebugging += safeNumber(review.get("debugging"));
                sumCommunication += safeNumber(review.get("communication"));
                sumSchedule += safeNumber(review.get("schedule"));
                sumDispute += safeNumber(review.get("dispute"));
            }

            int reviewCount = reviews.size();
            if (reviewCount == 0) {
                return new ReviewScoreSnapshot(0.0, 0.0, 0.0, 0.0, 0.0, 0.0);
            }

            return new ReviewScoreSnapshot(
                    sumProgramming / reviewCount,
                    sumFramework / reviewCount,
                    sumDebugging / reviewCount,
                    sumCommunication / reviewCount,
                    sumSchedule / reviewCount,
                    sumDispute / reviewCount
            );
        }

        private static double safeNumber(Object value) {
            if (value instanceof Number number) {
                return number.doubleValue();
            }
            return 0.0;
        }

        private double totalScore() {
            return (programming + framework + debugging + communication + schedule + dispute) / 6.0;
        }
    }
}
