package com.fallguys.mypage.service.employer;

import com.fallguys.mypage.api.web.dto.employer.response.EmployerReviewSummaryResponseDto;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmployerReviewServiceTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @InjectMocks
    private EmployerReviewService employerReviewService;

    @Test
    @DisplayName("고용주 평판 요약 조회: Redis에 등록된 리뷰 스코어 목록이 있을 때 평균을 정확히 계산한다")
    void getReputationSummary_Success() {
        // Given
        Long employerId = 1L;
        String redisKey = "employer:review:rates:" + employerId;
        
        // Mocking Data: List of Review Maps or DTOs.
        // 예를 들어 Review 도메인이 Redis에 아래와 같은 포맷으로 리뷰 점수 리스트를 올렸다고 가정합니다.
        List<Map<String, Double>> mockReviews = List.of(
            Map.of("atmosphereRate", 5.0, "requirementsDetailRate", 4.0, "scheduleAdherenceRate", 5.0),
            Map.of("atmosphereRate", 3.0, "requirementsDetailRate", 4.0, "scheduleAdherenceRate", 3.0),
            Map.of("atmosphereRate", 4.0, "requirementsDetailRate", 4.0, "scheduleAdherenceRate", 4.0)
        );

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
        
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(redisKey)).thenReturn(null);

        // When
        EmployerReviewSummaryResponseDto result = employerReviewService.getReputationSummary(employerId);

        // Then
        assertEquals(0.0, result.atmosphereRate());
        assertEquals(0.0, result.requirementsDetailRate());
        assertEquals(0.0, result.scheduleAdherenceRate());
        assertEquals(0.0, result.averageRate());
    }
}
