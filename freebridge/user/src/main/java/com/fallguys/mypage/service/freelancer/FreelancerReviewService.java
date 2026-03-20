package com.fallguys.mypage.service.freelancer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fallguys.common.ai.dto.FreelancerAiReputationReportDto;
import com.fallguys.common.ai.port.ReviewEngine;
import com.fallguys.common.event.ReputationUpdateRequestedEvent;
import com.fallguys.infra.ai.adapter.AiServiceException;
import com.fallguys.mypage.api.web.dto.freelancer.response.FreelancerAiPositivityIndexDto;
import com.fallguys.mypage.api.web.dto.freelancer.response.FreelancerEvaluationSummaryDto;
import com.fallguys.mypage.api.web.dto.freelancer.response.FreelancerStrengthWeaknessDto;
import com.fallguys.mypage.entity.freelancer.Collaboration;
import com.fallguys.mypage.entity.freelancer.Expertise;
import com.fallguys.mypage.entity.freelancer.Freelancer;
import com.fallguys.mypage.repository.freelancer.FreelancerRepository;
import com.fallguys.review.api.shared.ExternalFreelancerReviewApi;
import com.fallguys.review.api.shared.response.FreelancerReviewMetricsDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.Collections;

@Slf4j
@Service
@RequiredArgsConstructor
public class FreelancerReviewService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final FreelancerRepository freelancerRepository;
    private final ReviewEngine reviewEngine;
    private final ObjectMapper objectMapper;
    private final ExternalFreelancerReviewApi externalFreelancerReviewApi;

    @Transactional(readOnly = true)
    public FreelancerEvaluationSummaryDto getReviewSummary(Long userId) {
        Long freelancerId = resolveFreelancerId(userId);
        Integer topPercentile = getTopPercentile(userId);

        if (freelancerId == null) {
            log.warn("프리랜서 리뷰 요약 캐시 생성을 건너뜁니다. freelancerId를 찾지 못했습니다. userId={}", userId);
            return FreelancerEvaluationSummaryDto.empty(topPercentile);
        }

        try {
            log.info("Freelancer review summary requested. userId={}, freelancerId={}", userId, freelancerId);
            FreelancerReviewMetricsDto metrics = externalFreelancerReviewApi.getFreelancerReviewMetrics(userId);
            if (isEmptyMetrics(metrics)) {
                log.info("Freelancer review summary is empty. userId={}, freelancerId={}", userId, freelancerId);
                return FreelancerEvaluationSummaryDto.empty(topPercentile);
            }

            log.info(
                    "Freelancer review summary loaded metrics. userId={}, freelancerId={}, averageRate={}",
                    userId,
                    freelancerId,
                    metrics.averageRate()
            );
            return FreelancerEvaluationSummaryDto.fromAverageMap(
                    java.util.Map.of(
                            "programming", metrics.programming(),
                            "framework", metrics.framework(),
                            "debugging", metrics.debugging(),
                            "communication", metrics.communication(),
                            "schedule", metrics.schedule(),
                            "dispute", metrics.dispute()
                    ),
                    topPercentile
            );
        } catch (Exception e) {
            log.error("Redis에서 프리랜서 리뷰 요약을 파싱하지 못했습니다. userId={}, freelancerId={}", userId, freelancerId, e);
            return FreelancerEvaluationSummaryDto.empty(topPercentile);
        }
    }

    @Transactional
    public void syncReviewMetricsToFreelancer(Long userId) {
        Freelancer freelancer = freelancerRepository.findByUserId(userId).orElse(null);
        if (freelancer == null) {
            return;
        }

        Long freelancerId = freelancer.getFreelancerId();
        FreelancerReviewMetricsDto metrics = externalFreelancerReviewApi.getFreelancerReviewMetrics(userId);
        if (isEmptyMetrics(metrics)) {
            log.info("Freelancer profile sync found empty metrics. userId={}, freelancerId={}", userId, freelancerId);
            freelancer.updateReviewMetrics(
                    new Expertise(0.0, 0.0, 0.0),
                    new Collaboration(0.0, 0.0, 0.0),
                    0.0
            );
            return;
        }

        log.info(
                "Freelancer profile sync loaded metrics. userId={}, freelancerId={}, averageRate={}",
                userId,
                freelancerId,
                metrics.averageRate()
        );
        freelancer.updateReviewMetrics(
                new Expertise(metrics.programming(), metrics.framework(), metrics.debugging()),
                new Collaboration(metrics.communication(), metrics.schedule(), metrics.dispute()),
                metrics.averageRate()
        );
    }

    public FreelancerAiReputationReportDto getAiReputationReport(Long userId) {
        getReviewSummary(userId);

        Long freelancerId = resolveFreelancerId(userId);
        if (freelancerId == null) {
            log.warn("해당 userId에 대응하는 프리랜서 엔티티를 찾지 못했습니다. userId={}", userId);
            return emptyAiReport();
        }

        try {
            log.info("Freelancer AI report requested. userId={}, freelancerId={}", userId, freelancerId);
            FreelancerReviewMetricsDto metrics = externalFreelancerReviewApi.getFreelancerReviewMetrics(userId);
            if (isEmptyMetrics(metrics)) {
                log.info("Freelancer AI report skipped because metrics are empty. userId={}, freelancerId={}", userId, freelancerId);
                return new FreelancerAiReputationReportDto(
                        "미정",
                        0,
                        "아직 충분한 리뷰가 등록되지 않았습니다.",
                        Collections.emptyList(),
                        Collections.emptyList(),
                        Collections.emptyList(),
                        Collections.emptyList()
                );
            }
        } catch (Exception e) {
            log.warn("Redis에서 리뷰 존재 여부를 확인하지 못했습니다. freelancerId={}", freelancerId, e);
        }

        String redisKey = "freelancer:review:ai_report:" + userId;
        try {
            Object cachedData = redisTemplate.opsForValue().get(redisKey);
            if (cachedData != null) {
                log.info("Freelancer AI report cache hit. userId={}, freelancerId={}, redisKey={}", userId, freelancerId, redisKey);
                return objectMapper.convertValue(cachedData, FreelancerAiReputationReportDto.class);
            }
            log.info("Freelancer AI report cache miss. userId={}, freelancerId={}, redisKey={}", userId, freelancerId, redisKey);
        } catch (Exception e) {
            log.warn("Redis에서 AI 평판 리포트를 조회하지 못했습니다. freelancerId={}", freelancerId, e);
        }

        FreelancerAiReputationReportDto report;
        try {
            report = reviewEngine.getFreelancerAnalysis(userId);
        } catch (AiServiceException e) {
            log.warn("AI 평판 분석 서비스를 사용할 수 없습니다. userId={}, freelancerId={}", userId, freelancerId, e);
            return emptyAiReport();
        }

        try {
            if (report != null) {
                redisTemplate.opsForValue().set(redisKey, report, Duration.ofHours(24));
            }
        } catch (Exception e) {
            log.warn("AI 평판 리포트를 Redis에 저장하지 못했습니다. freelancerId={}", freelancerId, e);
        }

        return report != null ? report : emptyAiReport();
    }

    public FreelancerAiPositivityIndexDto getAiPositivityIndex(Long userId) {
        FreelancerAiReputationReportDto report = getAiReputationReport(userId);
        if (report == null) {
            return new FreelancerAiPositivityIndexDto(0.0, "POOR");
        }

        double totalScore = 0.0;
        int count = 0;

        if (report.technicalScores() != null) {
            for (FreelancerAiReputationReportDto.ScoreDto score : report.technicalScores()) {
                totalScore += score.score();
                count++;
            }
        }
        if (report.softSkills() != null) {
            for (FreelancerAiReputationReportDto.ScoreDto score : report.softSkills()) {
                totalScore += score.score();
                count++;
            }
        }

        if (count == 0) {
            return new FreelancerAiPositivityIndexDto(0.0, "POOR");
        }

        double posScore = (totalScore / count) * 20.0;
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

    private boolean isEmptyMetrics(FreelancerReviewMetricsDto metrics) {
        return metrics == null
                || (numberValue(metrics.programming()) == 0.0
                && numberValue(metrics.framework()) == 0.0
                && numberValue(metrics.debugging()) == 0.0
                && numberValue(metrics.communication()) == 0.0
                && numberValue(metrics.schedule()) == 0.0
                && numberValue(metrics.dispute()) == 0.0
                && numberValue(metrics.averageRate()) == 0.0);
    }

    private Integer getTopPercentile(Long userId) {
        try {
            return freelancerRepository.findByUserId(userId)
                    .map(Freelancer::getTopPercentile)
                    .orElse(null);
        } catch (Exception e) {
            log.warn("상위 백분위 값을 조회하지 못했습니다. userId={}", userId, e);
            return null;
        }
    }

    private Long resolveFreelancerId(Long userId) {
        try {
            return freelancerRepository.findByUserId(userId)
                    .map(Freelancer::getFreelancerId)
                    .orElse(null);
        } catch (Exception e) {
            log.warn("userId로 freelancerId를 찾지 못했습니다. userId={}", userId, e);
            return null;
        }
    }

    private FreelancerAiReputationReportDto emptyAiReport() {
        return new FreelancerAiReputationReportDto(
                "미정",
                0,
                "아직 충분한 리뷰가 등록되지 않았습니다.",
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList()
        );
    }

    private double numberValue(Double value) {
        return value == null ? 0.0 : value;
    }

    @Async
    @EventListener
    public void handleReputationUpdateRequested(ReputationUpdateRequestedEvent event) {
        if (event == null || event.freelancerId() == null) {
            log.warn("freelancerId가 없어 평판 갱신 이벤트를 건너뜁니다.");
            return;
        }

        Long freelancerId = event.freelancerId();
        String aiReportKey = "freelancer:review:ai_report:" + freelancerId;
        String ratesKey = "freelancer:review:rates:" + freelancerId;
        try {
            redisTemplate.delete(aiReportKey);
            redisTemplate.delete(ratesKey);
            log.info("프리랜서 리뷰 캐시 삭제 완료. freelancerId={}, aiReportKey={}, ratesKey={}", freelancerId, aiReportKey, ratesKey);
        } catch (Exception e) {
            log.warn("프리랜서 리뷰 캐시 삭제 실패. freelancerId={}, aiReportKey={}, ratesKey={}", freelancerId, aiReportKey, ratesKey, e);
        }

        freelancerRepository.findById(freelancerId)
                .map(Freelancer::getUserId)
                .ifPresent(userId -> {
                    String aiReportUserKey = "freelancer:review:ai_report:" + userId;
                    String ratesUserKey = "freelancer:review:rates:" + userId;
                    try {
                        redisTemplate.delete(aiReportUserKey);
                        redisTemplate.delete(ratesUserKey);
                        log.info("Freelancer review user cache invalidated. freelancerId={}, userId={}, aiReportUserKey={}, ratesUserKey={}",
                                freelancerId, userId, aiReportUserKey, ratesUserKey);
                    } catch (Exception e) {
                        log.warn("Failed to invalidate freelancer review user cache. freelancerId={}, userId={}, aiReportUserKey={}, ratesUserKey={}",
                                freelancerId, userId, aiReportUserKey, ratesUserKey, e);
                    }
                });
    }
}
