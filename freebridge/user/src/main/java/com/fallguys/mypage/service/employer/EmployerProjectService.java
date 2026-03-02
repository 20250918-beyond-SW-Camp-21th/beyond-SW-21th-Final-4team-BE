package com.fallguys.mypage.service.employer;

import com.fallguys.mypage.api.web.dto.employer.response.EmployerProjectStatsResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmployerProjectService {

    private final RedisTemplate<String, Object> redisTemplate;

    public EmployerProjectStatsResponseDto getProjectStats(Long employerId) {
        String redisKey = "employer:project:stats:" + employerId;
        
        try {
            Object rawData = redisTemplate.opsForValue().get(redisKey);
            
            if (rawData == null) {
                return EmployerProjectStatsResponseDto.empty();
            }

            @SuppressWarnings("unchecked")
            Map<String, Integer> stats = (Map<String, Integer>) rawData;

            return EmployerProjectStatsResponseDto.from(stats);

        } catch (Exception e) {
            log.error("Failed to parse employer project stats from Redis for employerId: {}", employerId, e);
            return EmployerProjectStatsResponseDto.empty();
        }
    }
}
