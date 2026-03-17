package com.fallguys.mypage.service.freelancer;

import com.fallguys.mypage.api.web.dto.freelancer.response.FreelancerEvaluationSummaryDto;
import com.fallguys.mypage.entity.freelancer.Freelancer;
import com.fallguys.mypage.repository.freelancer.FreelancerRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FreelancerReviewServiceTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @Mock
    private FreelancerRepository freelancerRepository;

    @Mock
    private EntityManager entityManager;

    @Mock
    private Query query;

    @InjectMocks
    private FreelancerReviewService freelancerReviewService;

    // ─── getReviewSummary ─────────────────────────────────────────

    @Test
    @DisplayName("[TDD] 1. 평판/등급 요약 조회: Redis에 리뷰 데이터가 있을 때 항목별 평균과 전체 평균을 계산하여 반환한다")
    void getReviewSummary_Success() {
        // given
        Long userId = 1L;
        String redisKey = "freelancer:review:rates:" + userId;
        Integer topPercentile = 15;

        Map<String, Object> mockAverages = Map.of(
                "programming", 4.0,
                "framework", 5.0,
                "debugging", 3.0,
                "communication", 5.0,
                "schedule", 4.0,
                "dispute", 2.0
        );

        Freelancer mockFreelancer = mock(Freelancer.class);
        given(mockFreelancer.getFreelancerId()).willReturn(userId);
        given(mockFreelancer.getTopPercentile()).willReturn(topPercentile);
        given(freelancerRepository.findByUserId(userId)).willReturn(Optional.of(mockFreelancer));
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.get(redisKey)).willReturn(mockAverages);

        // when
        FreelancerEvaluationSummaryDto result = freelancerReviewService.getReviewSummary(userId);

        // then
        assertThat(result.topPercentile()).isEqualTo(15);
        assertThat(result.expertiseRate()).isEqualTo(4.0);
        assertThat(result.communicationRate()).isEqualTo(5.0);
        assertThat(result.scheduleRate()).isEqualTo(4.0);
        // averageRate = (4.0+5.0+4.0)/3 = 4.3
        assertThat(result.averageRate()).isEqualTo(4.3);

        verify(redisTemplate, times(1)).opsForValue();
        verify(valueOperations, times(1)).get(redisKey);
    }

    @Test
    @DisplayName("[TDD] 2. 평판/등급 요약 조회: Redis 값이 null이면 topPercentile만 유지하고 나머지는 0인 DTO를 반환한다 (Fallback)")
    void getReviewSummary_RedisNull_ReturnsFallback() {
        // given
        Long userId = 2L;
        String redisKey = "freelancer:review:rates:" + userId;

        Freelancer mockFreelancer = mock(Freelancer.class);
        given(mockFreelancer.getFreelancerId()).willReturn(userId);
        given(mockFreelancer.getTopPercentile()).willReturn(30);
        given(freelancerRepository.findByUserId(userId)).willReturn(Optional.of(mockFreelancer));
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.get(redisKey)).willReturn(null);
        given(entityManager.createNativeQuery(anyString())).willReturn(query);
        given(query.setParameter("freelancerId", userId)).willReturn(query);
        given(query.getSingleResult()).willReturn(new Object[]{4.0, 5.0, 3.0, 5.0, 4.0, 2.0});

        // when
        FreelancerEvaluationSummaryDto result = freelancerReviewService.getReviewSummary(userId);

        // then
        assertThat(result.averageRate()).isEqualTo(4.3);
        assertThat(result.expertiseRate()).isEqualTo(4.0);
        assertThat(result.topPercentile()).isEqualTo(30);
        verify(valueOperations).set(eq(redisKey), any(Map.class));
    }

    @Test
    @DisplayName("[TDD] 3. 평판/등급 요약 조회: 레거시 List 포맷도 계속 읽을 수 있다")
    void getReviewSummary_LegacyListFormat_Success() {
        // given
        Long userId = 3L;
        String redisKey = "freelancer:review:rates:" + userId;

        Freelancer mockFreelancer = mock(Freelancer.class);
        given(mockFreelancer.getFreelancerId()).willReturn(userId);
        given(mockFreelancer.getTopPercentile()).willReturn(20);
        given(freelancerRepository.findByUserId(userId)).willReturn(Optional.of(mockFreelancer));
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.get(redisKey)).willReturn(List.of(
                Map.of("expertiseRate", 4.0, "communicationRate", 5.0, "scheduleRate", 3.0),
                Map.of("expertiseRate", 5, "communicationRate", 4, "scheduleRate", 4)
        ));

        // when
        FreelancerEvaluationSummaryDto result = freelancerReviewService.getReviewSummary(userId);

        // then
        assertThat(result.topPercentile()).isEqualTo(20);
        assertThat(result.expertiseRate()).isEqualTo(4.5);
        assertThat(result.communicationRate()).isEqualTo(4.5);
        assertThat(result.scheduleRate()).isEqualTo(3.5);
        assertThat(result.averageRate()).isEqualTo(4.2);
    }

    @Test
    @DisplayName("[TDD] 4. 평판/등급 요약 조회: Freelancer가 없으면 topPercentile은 null, 나머지는 0인 DTO를 반환한다")
    void getReviewSummary_FreelancerNotFound_ReturnsFallback() {
        // given
        Long userId = 4L;
        given(freelancerRepository.findByUserId(userId)).willReturn(Optional.empty());

        // when
        FreelancerEvaluationSummaryDto result = freelancerReviewService.getReviewSummary(userId);

        // then
        assertThat(result.topPercentile()).isNull();
        assertThat(result.averageRate()).isEqualTo(0.0);
    }
}
