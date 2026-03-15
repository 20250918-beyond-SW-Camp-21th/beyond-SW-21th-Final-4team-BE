package com.fallguys.mypage.service.employer;

import com.fallguys.common.ai.port.ReviewEngine;
import com.fallguys.mypage.api.web.dto.employer.response.EmployerReviewSummaryResponseDto;
import com.fallguys.mypage.entity.employer.Employer;
import com.fallguys.mypage.repository.employer.EmployerRepository;
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
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmployerReviewServiceTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @Mock
    private ReviewEngine reviewEngine;

    @Mock
    private EmployerRepository employerRepository;

    @Mock
    private EntityManager entityManager;

    @Mock
    private Query query;

    @Captor
    private ArgumentCaptor<List<Integer>> scoresCaptor;

    @Captor
    private ArgumentCaptor<List<String>> reviewsCaptor;

    @InjectMocks
    private EmployerReviewService employerReviewService;

    @Test
    @DisplayName("고용주 평판 요약 조회: Redis에 등록된 리뷰 스코어 목록이 있을 때 평균을 정확히 계산한다")
    void getReputationSummary_Success() {
        // Given
        Long employerId = 1L;
        String redisKey = "employer:review:rates:" + employerId;
        Employer employer = mock(Employer.class);
        
        // Mocking Data: List of Review Maps or DTOs.
        // 예를 들어 Review 도메인이 Redis에 아래와 같은 포맷으로 리뷰 점수 리스트를 올렸다고 가정합니다.
        List<Map<String, Object>> mockReviews = List.of(
            Map.of("atmosphereRate", 5, "requirementsDetailRate", 4.0, "scheduleAdherenceRate", 5),   // 정수/실수 혼합 검증
            Map.of("atmosphereRate", 3.0, "requirementsDetailRate", 4, "scheduleAdherenceRate", 3.0),
            Map.of("atmosphereRate", 4, "requirementsDetailRate", 4.0, "scheduleAdherenceRate", 4)
        );

        when(employer.getEmployerId()).thenReturn(employerId);
        when(employerRepository.findByUserId(employerId)).thenReturn(java.util.Optional.of(employer));
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(redisKey)).thenReturn(mockReviews);

        // When
        EmployerReviewSummaryResponseDto result = employerReviewService.getReputationSummary(employerId);

        // Then
        // atmosphereRate: (5+3+4)/3 = 4.0
        // requirementsDetailRate: (4+4+4)/3 = 4.0
        // scheduleAdherenceRate: (5+3+4)/3 = 4.0
        // averageRate (전체 평균): 4.0
        
        assertEquals(4.0, result.atmosphereRate());
        assertEquals(4.0, result.requirementsDetailRate());
        assertEquals(4.0, result.scheduleAdherenceRate());
        assertEquals(4.0, result.averageRate());
        
        verify(redisTemplate, times(1)).opsForValue();
        verify(valueOperations, times(1)).get(redisKey);
    }

    @Test
    @DisplayName("고용주 평판 요약 조회: 리뷰가 없을 경우 (Redis 값이 없음), 0.0을 반환해야 한다 (0으로 나누기 방지)")
    void getReputationSummary_EmptyReviews() {
        // Given
        Long employerId = 2L;
        String redisKey = "employer:review:rates:" + employerId;
        Employer employer = mock(Employer.class);
        
        when(employer.getEmployerId()).thenReturn(employerId);
        when(employerRepository.findByUserId(employerId)).thenReturn(java.util.Optional.of(employer));
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(redisKey)).thenReturn(null);
        when(entityManager.createNativeQuery(anyString())).thenReturn(query);
        when(query.setParameter("employerId", employerId)).thenReturn(query);
        when(query.getResultList()).thenReturn(List.of(
                new Object[]{5, 4.0, 5},
                new Object[]{3.0, 4, 3.0},
                new Object[]{4, 4.0, 4}
        ));

        // When
        EmployerReviewSummaryResponseDto result = employerReviewService.getReputationSummary(employerId);

        // Then
        assertEquals(4.0, result.atmosphereRate());
        assertEquals(4.0, result.requirementsDetailRate());
        assertEquals(4.0, result.scheduleAdherenceRate());
        assertEquals(4.0, result.averageRate());
        verify(valueOperations).set(eq(redisKey), anyList());
    }

    @Test
    @DisplayName("AI 신뢰도 점수 및 리포트 조회: ReviewEngine 연동 성공")
    void getAiReputation_Success() {
        // Given
        Long userId = 1L;
        // Mocking the data passed to AI
        List<Integer> mockScores = List.of(5, 4, 3, 5);
        List<String> mockReviews = List.of("좋아요", "그냥 그래요", "별로예요", "최고에요");

        Map<String, Object> mockAiResult = Map.of(
                "summary", "전체적으로 우수한 평가를 받고 있습니다.",
                "positive_keywords", List.of("신속함", "정확함"),
                "negative_keywords", List.of("의사소통")
        );

        when(reviewEngine.analyzeReputation(anyList(), anyList())).thenReturn(mockAiResult);

        // When
        var result = employerReviewService.getAiReputation(userId);

        // Then
        assertEquals("전체적으로 우수한 평가를 받고 있습니다.", result.aiSummary());
        assertEquals(2, result.positiveKeywords().size());
        assertEquals(1, result.negativeKeywords().size());
        assertEquals("신속함", result.positiveKeywords().get(0));

        verify(reviewEngine, times(1)).analyzeReputation(scoresCaptor.capture(), reviewsCaptor.capture());
        
        List<Integer> capturedScores = scoresCaptor.getValue();
        List<String> capturedReviews = reviewsCaptor.getValue();
        
        assertEquals(mockScores, capturedScores);
        assertEquals(List.of("좋아요", "무난합니다", "아쉽네요", "최고에요"), capturedReviews);
    }

    @Test
    @DisplayName("AI 신뢰도 점수 및 리포트 조회: ReviewEngine 예외 발생 시 안전하게 빈 DTO 반환 (Fallback)")
    void getAiReputation_Fallback() {
        // Given
        Long userId = 2L;

        when(reviewEngine.analyzeReputation(anyList(), anyList()))
                .thenThrow(new RuntimeException("AI 분석 서비스 응답 오류"));

        // When
        var result = employerReviewService.getAiReputation(userId);

        // Then
        // Should return a default empty structure instead of crashing
        assertEquals("AI 리포트를 불러올 수 없습니다.", result.aiSummary());
        assertEquals(0, result.positiveKeywords().size());
        assertEquals(0, result.negativeKeywords().size());
    }
}
