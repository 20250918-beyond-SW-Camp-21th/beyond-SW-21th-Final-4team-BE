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

        // 3. Employer Project List Data
        String listKey = "employer:project:list:1";
        java.util.List<java.util.Map<String, Object>> mockList = java.util.List.of(
            java.util.Map.of("projectId", 101, "title", "웹 에이전시 구축 프로젝트", "status", "모집중", "applicantCount", 5, "createdAt", "2026-03-01T10:00:00", "deadline", "2026-03-15T23:59:59"),
            java.util.Map.of("projectId", 102, "title", "안드로이드 앱 유지보수", "status", "진행중", "applicantCount", 12, "createdAt", "2026-02-15T09:00:00", "deadline", "2026-02-28T23:59:59")
        );
        redisTemplate.opsForValue().set(listKey, mockList);

        return "Successfully injected mock data for User ID 1 into Cloud Redis!";
    }
}
