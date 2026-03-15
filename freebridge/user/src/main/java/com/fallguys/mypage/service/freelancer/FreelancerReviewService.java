package com.fallguys.mypage.service.freelancer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fallguys.common.ai.dto.FreelancerAiReputationReportDto;
import com.fallguys.common.ai.port.ReviewEngine;
import com.fallguys.infra.ai.adapter.AiServiceException;
import com.fallguys.mypage.api.web.dto.freelancer.response.FreelancerAiPositivityIndexDto;
import com.fallguys.mypage.api.web.dto.freelancer.response.FreelancerEvaluationSummaryDto;
import com.fallguys.mypage.api.web.dto.freelancer.response.FreelancerStrengthWeaknessDto;
import com.fallguys.mypage.entity.freelancer.Freelancer;
import com.fallguys.mypage.repository.freelancer.FreelancerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
     * Expected value:
     * { programming, framework, debugging, communication, schedule, dispute }
     * topPercentile은 Freelancer 엔티티에서 조회합니다.
     */
    @Transactional(readOnly = true)
    public FreelancerEvaluationSummaryDto getReviewSummary(Long userId) {
        Long freelancerId = resolveFreelancerId(userId);
        Integer topPercentile = getTopPercentile(userId);

        if (freelancerId == null) {
          return FreelancerEvaluationSummaryDto.empty(topPercentile);
          }

        String redisKey = "freelancer:review:rates:" + freelancerId;

        try {
            Object rawData = redisTemplate.opsForValue().get(redisKey);
            if (rawData == null) {
                return FreelancerEvaluationSummaryDto.empty(topPercentile);
            }
            if (rawData instanceof Map<?, ?> rawMap) {
                @SuppressWarnings("unchecked")
                Map<String, Object> averages = (Map<String, Object>) rawMap;
                return FreelancerEvaluationSummaryDto.fromAverageMap(averages, topPercentile);
            }
            if (rawData instanceof List<?> rawList) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> reviews = (List<Map<String, Object>>) rawList;
                return FreelancerEvaluationSummaryDto.from(reviews, topPercentile);
            }
            return FreelancerEvaluationSummaryDto.empty(topPercentile);

        } catch (Exception e) {
            log.error(
            "Redis에서 프리랜서 리뷰 요약을 파싱하지 못했습니다. userId={}, freelancerId={}",
             userId,
              freelancerId,
              e
            );
            return FreelancerEvaluationSummaryDto.empty(topPercentile);
        }
    }

    public FreelancerAiReputationReportDto getAiReputationReport(Long userId) {
        Long freelancerId = resolveFreelancerId(userId);
        if (freelancerId == null) {
            log.warn("해당 userId에 대한 프리랜서 엔티티를 찾지 못했습니다. userId={}", userId);
            return emptyAiReport();
        }

        String ratesRedisKey = "freelancer:review:rates:" + freelancerId;
        try {
            Object ratesData = redisTemplate.opsForValue().get(ratesRedisKey);
            if (ratesData instanceof List<?> list && list.isEmpty()) {
                return emptyAiReport();
            }
            if (ratesData instanceof Map<?, ?> map && map.isEmpty()) {
                return new FreelancerAiReputationReportDto(
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

        String redisKey = "freelancer:review:ai_report:" + freelancerId;
        try {
            Object cachedData = redisTemplate.opsForValue().get(redisKey);
            if (cachedData != null) {
                return objectMapper.convertValue(cachedData, FreelancerAiReputationReportDto.class);
            }
        } catch (Exception e) {
            log.warn("Redis에서 AI 평판 리포트를 조회하지 못했습니다. freelancerId={}", freelancerId, e);
        }

        FreelancerAiReputationReportDto report;
        try {
            report = reviewEngine.getFreelancerAnalysis(freelancerId);
        } catch (AiServiceException e) {
            log.warn("AI 평판 분석을 사용할 수 없습니다. userId={}, freelancerId={}", userId, freelancerId, e);
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
                "아직 충분한 리뷰가 등록되지 않았습니다.",
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList()
        );
    }
}
