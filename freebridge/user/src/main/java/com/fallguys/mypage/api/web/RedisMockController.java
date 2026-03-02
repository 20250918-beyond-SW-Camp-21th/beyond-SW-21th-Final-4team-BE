package com.fallguys.mypage.api.web;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/employer/mypage/test")
@RequiredArgsConstructor
public class RedisMockController {

    private final RedisTemplate<String, Object> redisTemplate;

    @GetMapping("/inject-mock-data")
    public String injectMockData() {
        // 1. Employer Review Summary Data
        String reviewKey = "employer:review:rates:1";
        redisTemplate.opsForValue().set(reviewKey, 
            "[{\"atmosphereRate\": 4.5, \"requirementsDetailRate\": 4.0, \"scheduleAdherenceRate\": 5.0}, {\"atmosphereRate\": 3.5, \"requirementsDetailRate\": 4.5, \"scheduleAdherenceRate\": 4.5}]");

        // 2. Employer Project Stats Data
        String statsKey = "employer:project:stats:1";
        redisTemplate.opsForValue().set(statsKey, 
            "{\"totalProjects\": 15, \"activeApplicants\": 3, \"contractedFreelancers\": 7}");

        return "Successfully injected mock data for User ID 1 into Cloud Redis!";
    }
}
