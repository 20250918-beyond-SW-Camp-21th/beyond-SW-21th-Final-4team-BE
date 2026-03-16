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

import java.util.Collections;
import java.util.List;
import java.util.HashMap;
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

    private EmployerReviewSummaryResponseDto buildAndCacheSummary(Long employerId, String redisKey) {
        Query query = entityManager.createNativeQuery("""
                SELECT atmosphere, requirement_detail, schedule
                FROM freelancer_employer_reviews
                WHERE employer_id = :employerId
                  AND status = 'ACTIVE'
                  AND deleted = false
                """);
        query.setParameter("employerId", employerId);

        @SuppressWarnings("unchecked")
        List<Object[]> rows = query.getResultList();
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

    private void cacheEmployerReviewSummary(String redisKey, Map<String, Object> payload) {
        try {
            redisTemplate.opsForValue().set(redisKey, payload);
        } catch (Exception e) {
            log.warn("고용주 리뷰 요약을 Redis에 저장하지 못했습니다. key={}", redisKey, e);
        }
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

    public EmployerReputationAiResponseDto getAiReputation(Long userId) {
        try {
            // TODO: 실제 Review DB에서 해당 고용주의 평가 점수 및 텍스트 리스트를 가져오는 로직 추가 필요.
            // 현재는 API 프록시 통신 및 파싱 성공 여부를 테스트하기 위해 더미 데이터 주입
            List<Integer> mockScores = List.of(5, 4, 3, 5);
            List<String> mockReviews = List.of("좋아요", "무난합니다", "아쉽네요", "최고에요");

            Map<String, Object> aiResult = reviewEngine.analyzeReputation(mockScores, mockReviews);
            
            String summary = (String) aiResult.getOrDefault("summary", "분석된 요약이 없습니다.");
            
            @SuppressWarnings("unchecked")
            List<String> positive = (List<String>) aiResult.getOrDefault("positive_keywords", Collections.emptyList());
            
            @SuppressWarnings("unchecked")
            List<String> negative = (List<String>) aiResult.getOrDefault("negative_keywords", Collections.emptyList());

            return new EmployerReputationAiResponseDto(summary, positive, negative);

        } catch (Exception e) {
            log.error("Failed to fetch AI Reputation for employerId: {}", userId, e);
            return new EmployerReputationAiResponseDto(
                    "AI 리포트를 불러올 수 없습니다.",
                    Collections.emptyList(),
                    Collections.emptyList()
            );
        }
    }
}
