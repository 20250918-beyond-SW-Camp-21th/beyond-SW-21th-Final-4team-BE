package com.fallguys.mypage.service.employer;

import com.fallguys.mypage.api.web.dto.employer.response.EmployerSubscriptionResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmployerAccountService {

    private final RedisTemplate<String, Object> redisTemplate;

    public EmployerSubscriptionResponseDto getSubscription(Long employerId) {
        String redisKey = "employer:subscription:" + employerId;

        try {
            Object rawData = redisTemplate.opsForValue().get(redisKey);

            if (rawData == null) {
                return new EmployerSubscriptionResponseDto(null, null, null);
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) rawData;

            String currentPlan = data.get("currentPlan") != null ? data.get("currentPlan").toString() : null;
            
            @SuppressWarnings("unchecked")
            List<String> features = data.get("features") != null ? (List<String>) data.get("features") : null;
            
            LocalDateTime nextBillingDate = data.get("nextBillingDate") != null ? LocalDateTime.parse(data.get("nextBillingDate").toString()) : null;

            return new EmployerSubscriptionResponseDto(currentPlan, features, nextBillingDate);

        } catch (Exception e) {
            log.error("Failed to parse employer subscription from Redis for employerId: {}", employerId, e);
            return new EmployerSubscriptionResponseDto(null, null, null);
        }
    }
}
