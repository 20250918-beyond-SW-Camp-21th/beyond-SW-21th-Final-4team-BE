package com.fallguys.mypage.service.employer;

import com.fallguys.common.ai.port.ReviewEngine;
import com.fallguys.mypage.api.web.dto.employer.response.EmployerReputationAiResponseDto;
import com.fallguys.mypage.api.web.dto.employer.response.EmployerReviewSummaryResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmployerReviewService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ReviewEngine reviewEngine;

    public EmployerReviewSummaryResponseDto getReputationSummary(Long userId) {
        String redisKey = "employer:review:rates:" + userId;
        
        try {
            Object rawData = redisTemplate.opsForValue().get(redisKey);
            
            if (rawData == null) {
                return EmployerReviewSummaryResponseDto.empty();
            }

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> reviews = (List<Map<String, Object>>) rawData;

            return EmployerReviewSummaryResponseDto.from(reviews);

        } catch (Exception e) {
            log.error("Failed to parse employer review summary from Redis for employerId: {}", userId, e);
            return EmployerReviewSummaryResponseDto.empty();
        }
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
