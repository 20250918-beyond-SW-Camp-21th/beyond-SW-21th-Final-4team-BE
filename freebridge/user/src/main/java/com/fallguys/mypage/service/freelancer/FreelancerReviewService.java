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
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.fallguys.common.event.ReputationUpdateRequestedEvent;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class FreelancerReviewService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final FreelancerRepository freelancerRepository;
    private final EntityManager entityManager;
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
          log.warn("프리랜서 리뷰 요약 캐시 생성을 건너뜁니다. freelancerId를 찾지 못했습니다. userId={}", userId);
          return FreelancerEvaluationSummaryDto.empty(topPercentile);
        }

        String redisKey = "freelancer:review:rates:" + freelancerId;

        try {
            Object rawData = redisTemplate.opsForValue().get(redisKey);
            if (rawData == null) {
                log.info("프리랜서 리뷰 요약 Redis miss. userId={}, freelancerId={}, key={}", userId, freelancerId, redisKey);
                return buildAndCacheReviewSummary(freelancerId, topPercentile, redisKey);
            }
            log.info("프리랜서 리뷰 요약 Redis hit. userId={}, freelancerId={}, key={}", userId, freelancerId, redisKey);
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

    private FreelancerEvaluationSummaryDto buildAndCacheReviewSummary(Long freelancerId, Integer topPercentile, String redisKey) {
        Query query = entityManager.createNativeQuery("""
                SELECT
                    AVG(language),
                    AVG(framework),
                    AVG(debugging),
                    AVG(communication),
                    AVG(schedule),
                    AVG(dispute)
                FROM employer_freelancer_reviews
                WHERE freelancer_id = :freelancerId
                  AND status = 'ACTIVE'
                  AND deleted = false
                """);
        query.setParameter("freelancerId", freelancerId);

        Object[] row = (Object[]) query.getSingleResult();
        if (row == null || isAllNull(row)) {
            cacheFreelancerReviewSummary(redisKey, Map.of());
            return FreelancerEvaluationSummaryDto.empty(topPercentile);
        }

        Map<String, Object> averages = new HashMap<>();
        averages.put("programming", round1(numberValue(row[0])));
        averages.put("framework", round1(numberValue(row[1])));
        averages.put("debugging", round1(numberValue(row[2])));
        averages.put("communication", round1(numberValue(row[3])));
        averages.put("schedule", round1(numberValue(row[4])));
        averages.put("dispute", round1(numberValue(row[5])));

        cacheFreelancerReviewSummary(redisKey, averages);
        return FreelancerEvaluationSummaryDto.fromAverageMap(averages, topPercentile);
    }

    private void cacheFreelancerReviewSummary(String redisKey, Map<String, Object> averages) {
        try {
            redisTemplate.opsForValue().set(redisKey, averages);
            log.info("프리랜서 리뷰 요약 Redis 저장 완료. key={}, empty={}", redisKey, averages.isEmpty());
        } catch (Exception e) {
            log.warn("프리랜서 리뷰 요약을 Redis에 저장하지 못했습니다. key={}", redisKey, e);
        }
    }

    public FreelancerAiReputationReportDto getAiReputationReport(Long userId) {
        log.info("프리랜서 AI 평판 조회 요청. userId={}", userId);
        getReviewSummary(userId);

        Long freelancerId = resolveFreelancerId(userId);
        log.info("프리랜서 AI 평판 조회 매핑 결과. userId={}, freelancerId={}", userId, freelancerId);
        if (freelancerId == null) {
            log.warn("해당 userId에 대한 프리랜서 엔티티를 찾지 못했습니다. userId={}", userId);
            return emptyAiReport();
        }

        String ratesRedisKey = "freelancer:review:rates:" + freelancerId;
        try {
            Object ratesData = redisTemplate.opsForValue().get(ratesRedisKey);
            if (ratesData == null) {
                log.info("프리랜서 리뷰 rates 캐시 miss. userId={}, freelancerId={}, key={}", userId, freelancerId, ratesRedisKey);
            } else {
                log.info(
                        "프리랜서 리뷰 rates 캐시 hit. userId={}, freelancerId={}, key={}, valueType={}, empty={}",
                        userId,
                        freelancerId,
                        ratesRedisKey,
                        ratesData.getClass().getSimpleName(),
                        isEmptyCacheValue(ratesData)
                );
            }
            if (ratesData instanceof List<?> list && list.isEmpty()) {
                log.info("프리랜서 AI 평판 기본 응답 반환. 이유=empty_list_rates_cache, userId={}, freelancerId={}", userId, freelancerId);
                return emptyAiReport();
            }
            if (ratesData instanceof Map<?, ?> map && map.isEmpty()) {
                log.info("프리랜서 AI 평판 기본 응답 반환. 이유=empty_map_rates_cache, userId={}, freelancerId={}", userId, freelancerId);
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
            log.warn("Redis에서 리뷰 존재 여부를 확인하지 못했습니다. userId={}, freelancerId={}, key={}", userId, freelancerId, ratesRedisKey, e);
        }

        String redisKey = "freelancer:review:ai_report:" + freelancerId;
        try {
            Object cachedData = redisTemplate.opsForValue().get(redisKey);
            if (cachedData != null) {
                log.info(
                        "프리랜서 AI 평판 리포트 캐시 hit. userId={}, freelancerId={}, key={}, valueType={}",
                        userId,
                        freelancerId,
                        redisKey,
                        cachedData.getClass().getSimpleName()
                );
                return objectMapper.convertValue(cachedData, FreelancerAiReputationReportDto.class);
            }
            log.info("프리랜서 AI 평판 리포트 캐시 miss. userId={}, freelancerId={}, key={}", userId, freelancerId, redisKey);
        } catch (Exception e) {
            log.warn("Redis에서 AI 평판 리포트를 조회하지 못했습니다. userId={}, freelancerId={}, key={}", userId, freelancerId, redisKey, e);
        }

        FreelancerAiReputationReportDto report;
        try {
            log.info("Python AI 평판 분석 호출 시작. userId={}, freelancerId={}", userId, freelancerId);
            report = reviewEngine.getFreelancerAnalysis(freelancerId);
        } catch (AiServiceException e) {
            log.warn("AI 평판 분석을 사용할 수 없습니다. userId={}, freelancerId={}", userId, freelancerId, e);
            return emptyAiReport();
        }

        try {
            if (report != null) {
                redisTemplate.opsForValue().set(redisKey, report, Duration.ofHours(24));
                log.info("프리랜서 AI 평판 리포트 Redis 저장 완료. userId={}, freelancerId={}, key={}", userId, freelancerId, redisKey);
            }
        } catch (Exception e) {
            log.warn("AI 평판 리포트를 Redis에 저장하지 못했습니다. userId={}, freelancerId={}, key={}", userId, freelancerId, redisKey, e);
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

    // ─── 내부 헬퍼 ──────────────────────────────────────────────

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

    private boolean isAllNull(Object[] row) {
        for (Object value : row) {
            if (value != null) {
                return false;
            }
        }
        return true;
    }

    private double numberValue(Object value) {
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        return 0.0;
    }

    private double round1(double value) {
        return Math.round(value * 10.0) / 10.0;
    }

    private boolean isEmptyCacheValue(Object value) {
        if (value instanceof Map<?, ?> map) {
            return map.isEmpty();
        }
        if (value instanceof List<?> list) {
            return list.isEmpty();
        }
        return false;
    }

    @Async
    @EventListener
    public void handleReputationUpdateRequested(ReputationUpdateRequestedEvent event) {
        if (event == null || event.freelancerId() == null) {
            log.warn("Reputation update event skipped because freelancerId is null.");
            return;
        }

        Long freelancerId = event.freelancerId();
        String redisKey = "freelancer:review:ai_report:" + freelancerId;
        try {
            redisTemplate.delete(redisKey);
            log.info("프리랜서 AI 평판 리포트 캐시 삭제 완료. freelancerId={}, key={}", freelancerId, redisKey);
        } catch (Exception e) {
            log.warn("프리랜서 AI 평판 리포트 캐시 삭제 실패. freelancerId={}, key={}", freelancerId, redisKey, e);
        }
    }
}
