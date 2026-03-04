package com.fallguys.mypage.service.freelancer;

import com.fallguys.mypage.api.web.dto.freelancer.response.FreelancerAiPositivityIndexDto;
import com.fallguys.mypage.api.web.dto.freelancer.response.FreelancerAiReputationReportDto;
import com.fallguys.mypage.api.web.dto.freelancer.response.FreelancerEvaluationSummaryDto;
import com.fallguys.mypage.api.web.dto.freelancer.response.FreelancerStrengthWeaknessDto;
import com.fallguys.mypage.entity.freelancer.Freelancer;
import com.fallguys.mypage.repository.freelancer.FreelancerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class FreelancerReviewService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final FreelancerRepository freelancerRepository;

    /**
     * 내 평판/등급 요약 조회
     * Redis Key: freelancer:review:rates:{freelancerId}
     * Expected value: List<Map<String, Double>> { expertiseRate, communicationRate, scheduleRate }
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
     * AI 평판 분석 리포트 조회 (뼈대 - 향후 AI 도메인 연동 예정)
     */
    public FreelancerAiReputationReportDto getAiReputationReport(Long userId) {
        // TODO: AI 도메인에서 프리랜서 리뷰 데이터를 받아 분석 결과를 조회하는 로직 구현 예정
        return new FreelancerAiReputationReportDto(
                "AI 분석 리포트 준비 중입니다.",
                Collections.emptyList(),
                Collections.emptyList()
        );
    }

    /**
     * AI 평판 긍정 지수 조회 (뼈대 - AI 도메인에서 항목별 평판 점수를 받아 조회 예정)
     */
    public FreelancerAiPositivityIndexDto getAiPositivityIndex(Long userId) {
        // TODO: AI 도메인에서 전달된 긍정 지수(positivityScore) 및 등급(grade)을 조회하는 로직 구현 예정
        return new FreelancerAiPositivityIndexDto(null, null);
    }

    /**
     * 프리랜서 강점/약점 분석 조회 (뼈대 - AI 도메인 연동 예정)
     * AI가 분석한 강점 3가지, 약점 3가지를 반환합니다.
     */
    public FreelancerStrengthWeaknessDto getStrengthWeaknessAnalysis(Long userId) {
        // TODO: AI 도메인에서 전달한 평점/리뷰 데이터를 바탕으로 강점/약점 항목 조회 로직 구현 예정
        return new FreelancerStrengthWeaknessDto(
                Collections.emptyList(), // 강점 최대 3가지 (예: "전문성 우수", "의사소통 원활", "일정준수")
                Collections.emptyList()  // 약점 최대 3가지
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
