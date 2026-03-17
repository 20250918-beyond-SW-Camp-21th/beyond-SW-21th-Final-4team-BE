package com.fallguys.mypage.service.employer;

import com.fallguys.common.ai.port.ReviewEngine;
import com.fallguys.mypage.api.web.dto.employer.response.EmployerReputationAiResponseDto;
import com.fallguys.mypage.api.web.dto.employer.response.EmployerReviewSummaryResponseDto;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmployerReviewService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final EntityManager entityManager;
    private final ReviewEngine reviewEngine;

    public EmployerReviewSummaryResponseDto getReputationSummary(Long userId) {
        Long employerId = userId;
        
        String redisKey = "employer:review:rates:" + employerId;

        try {
            Object rawData = redisTemplate.opsForValue().get(redisKey);

            if (rawData == null) {
                return buildAndCacheSummary(employerId, redisKey);
            }

            if (rawData instanceof Map<?, ?> rawMap) {
                @SuppressWarnings("unchecked")
                Map<String, Object> averages = (Map<String, Object>) rawMap;
                return EmployerReviewSummaryResponseDto.fromAverageMap(averages);
            }

            if (rawData instanceof List<?> rawList) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> reviews = (List<Map<String, Object>>) rawList;
                return EmployerReviewSummaryResponseDto.from(reviews);
            }

            return EmployerReviewSummaryResponseDto.empty();
        } catch (Exception e) {
            log.error("Failed to parse employer review summary from Redis for employerId: {}", userId, e);
            return EmployerReviewSummaryResponseDto.empty();
        }
    }

    public EmployerReputationAiResponseDto getAiReputation(Long userId) {
        Long employerId = userId;
        log.info("Employer reputation AI requested. employerId={}", employerId);

        try {
            List<Object[]> rows = fetchEmployerReviewRows(employerId, true);
            if (rows == null || rows.isEmpty()) {
                log.info("Employer reputation AI skipped. employerId={}, reason=no_reviews", employerId);
                return emptyAiReputation("리뷰가 없어 AI 평가를 생성할 수 없습니다.");
            }

            List<Integer> scores = new ArrayList<>();
            List<String> reviews = new ArrayList<>();
            for (Object[] row : rows) {
                scores.add(toScore(row[0]));
                scores.add(toScore(row[1]));
                scores.add(toScore(row[2]));

                if (row.length > 3 && row[3] instanceof String description && !description.isBlank()) {
                    reviews.add(description.trim());
                }
            }

            if (reviews.isEmpty()) {
                reviews.add("리뷰 코멘트 없음");
            }

            Map<String, Object> aiResult = reviewEngine.analyzeReputation(scores, reviews);
            String summary = (String) aiResult.getOrDefault("summary", "AI 리포트를 불러올 수 없습니다.");

            @SuppressWarnings("unchecked")
            List<String> positive = (List<String>) aiResult.getOrDefault("positive_keywords", Collections.emptyList());

            @SuppressWarnings("unchecked")
            List<String> negative = (List<String>) aiResult.getOrDefault("negative_keywords", Collections.emptyList());

            log.info(
                    "Employer reputation AI generated. employerId={}, reviewCount={}, scoreCount={}, positiveCount={}, negativeCount={}",
                    employerId,
                    rows.size(),
                    scores.size(),
                    positive.size(),
                    negative.size()
            );

            return new EmployerReputationAiResponseDto(summary, positive, negative);
        } catch (Exception e) {
            log.error("Failed to fetch AI Reputation for employerId: {}", employerId, e);
            return emptyAiReputation("AI 리포트를 불러올 수 없습니다.");
        }
    }

    private EmployerReviewSummaryResponseDto buildAndCacheSummary(Long employerId, String redisKey) {
        List<Object[]> rows = fetchEmployerReviewRows(employerId, false);
        if (rows == null || rows.isEmpty()) {
            cacheEmployerReviewSummary(redisKey, Map.of());
            return EmployerReviewSummaryResponseDto.empty();
        }

        double sumAtmosphere = 0.0;
        double sumRequirements = 0.0;
        double sumSchedule = 0.0;
        int count = 0;

        for (Object[] row : rows) {
            sumAtmosphere += numberValue(row[0]);
            sumRequirements += numberValue(row[1]);
            sumSchedule += numberValue(row[2]);
            count++;
        }

        Map<String, Object> averages = new HashMap<>();
        averages.put("atmosphereRate", round1(sumAtmosphere / count));
        averages.put("requirementsDetailRate", round1(sumRequirements / count));
        averages.put("scheduleAdherenceRate", round1(sumSchedule / count));

        cacheEmployerReviewSummary(redisKey, averages);
        return EmployerReviewSummaryResponseDto.fromAverageMap(averages);
    }

    private List<Object[]> fetchEmployerReviewRows(Long employerId, boolean includeDescription) {
        String selectClause = includeDescription
                ? "SELECT atmosphere, requirement_detail, schedule, description "
                : "SELECT atmosphere, requirement_detail, schedule ";

        Query query = entityManager.createNativeQuery(
                selectClause +
                        """
                        FROM freelancer_employer_reviews
                        WHERE employer_id = :employerId
                          AND status = 'ACTIVE'
                          AND deleted = false
                        """
        );
        query.setParameter("employerId", employerId);

        @SuppressWarnings("unchecked")
        List<Object[]> rows = query.getResultList();
        return rows;
    }

    private void cacheEmployerReviewSummary(String redisKey, Map<String, Object> payload) {
        try {
            redisTemplate.opsForValue().set(redisKey, payload);
        } catch (Exception e) {
            log.warn("고용주 리뷰 요약을 Redis에 저장하지 못했습니다. key={}", redisKey, e);
        }
    }

    private EmployerReputationAiResponseDto emptyAiReputation(String message) {
        return new EmployerReputationAiResponseDto(
                message,
                Collections.emptyList(),
                Collections.emptyList()
        );
    }

    private int toScore(Object value) {
        return (int) Math.round(numberValue(value));
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
}
