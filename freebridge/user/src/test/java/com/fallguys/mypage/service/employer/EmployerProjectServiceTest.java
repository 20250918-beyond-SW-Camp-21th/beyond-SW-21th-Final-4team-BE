package com.fallguys.mypage.service.employer;

import com.fallguys.mypage.api.web.dto.employer.response.EmployerProjectStatsResponseDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmployerProjectServiceTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @InjectMocks
    private EmployerProjectService employerProjectService;

    @Test
    @DisplayName("고용주 프로젝트 통계 조회: Redis에 등록된 통계 정보가 있을 때 매핑이 정상적으로 수행된다")
    void getProjectStats_Success() {
        // Given
        Long employerId = 1L;
        String redisKey = "employer:project:stats:" + employerId;

        Map<String, Integer> mockStats = Map.of(
            "totalProjects", 10,
            "activeApplicants", 5,
            "contractedFreelancers", 15
        );

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(redisKey)).thenReturn(mockStats);

        // When
        EmployerProjectStatsResponseDto result = employerProjectService.getProjectStats(employerId);

        // Then
        assertEquals(10, result.totalProjects());
        assertEquals(5, result.activeApplicants());
        assertEquals(15, result.contractedFreelancers());

        verify(redisTemplate, times(1)).opsForValue();
        verify(valueOperations, times(1)).get(redisKey);
    }

    @Test
    @DisplayName("고용주 프로젝트 통계 조회: Redis 값이 없거나 null인 경우 0으로 채워진 DTO를 반환한다 (Fallback)")
    void getProjectStats_Empty() {
        // Given
        Long employerId = 2L;
        String redisKey = "employer:project:stats:" + employerId;

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(redisKey)).thenReturn(null);

        // When
        EmployerProjectStatsResponseDto result = employerProjectService.getProjectStats(employerId);

        // Then
        assertEquals(0, result.totalProjects());
        assertEquals(0, result.activeApplicants());
        assertEquals(0, result.contractedFreelancers());
    }
}
