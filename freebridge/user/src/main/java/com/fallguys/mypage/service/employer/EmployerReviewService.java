package com.fallguys.mypage.service.employer;

import com.fallguys.mypage.api.web.dto.employer.response.EmployerReviewSummaryResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmployerReviewService {

    private final RedisTemplate<String, Object> redisTemplate;

    public EmployerReviewSummaryResponseDto getReputationSummary(Long employerId) {
        String redisKey = "employer:review:rates:" + employerId;
        
        try {
            Object rawData = redisTemplate.opsForValue().get(redisKey);
            
            if (rawData == null) {
                return EmployerReviewSummaryResponseDto.empty();
            }

            @SuppressWarnings("unchecked")
            List<Map<String, Double>> reviews = (List<Map<String, Double>>) rawData;

            return EmployerReviewSummaryResponseDto.from(reviews);

        } catch (Exception e) {
            log.error("Failed to parse employer review summary from Redis for employerId: {}", employerId, e);
            return EmployerReviewSummaryResponseDto.empty();
        }
    }
}
