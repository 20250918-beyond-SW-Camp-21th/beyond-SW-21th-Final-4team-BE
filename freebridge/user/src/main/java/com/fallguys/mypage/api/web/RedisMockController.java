package com.fallguys.mypage.api.web;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/employer/mypage/test")
@RequiredArgsConstructor
@Profile({"local", "dev"})
public class RedisMockController {

    private final RedisTemplate<String, Object> redisTemplate;

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/inject-mock-data")
    public String injectMockData() {
        // 1. Employer Review Summary Data
        String reviewKey = "employer:review:rates:1";
        List<Map<String, Double>> mockReviews = List.of(
            Map.of("atmosphereRate", 4.5, "requirementsDetailRate", 4.0, "scheduleAdherenceRate", 5.0),
            Map.of("atmosphereRate", 3.5, "requirementsDetailRate", 4.5, "scheduleAdherenceRate", 4.5)
        );
        redisTemplate.opsForValue().set(reviewKey, mockReviews);

        // 2. Employer Project Stats Data
        String statsKey = "employer:project:stats:1";
        Map<String, Integer> mockStats = Map.of(
            "totalProjects", 15, "activeApplicants", 3, "contractedFreelancers", 7
        );
        redisTemplate.opsForValue().set(statsKey, mockStats);

        // 3. Employer Project List Data
        String listKey = "employer:project:list:1";
        java.util.List<java.util.Map<String, Object>> mockList = java.util.List.of(
            java.util.Map.of("projectId", 101, "title", "웹 에이전시 구축 프로젝트", "status", "모집중", "applicantCount", 5, "createdAt", "2026-03-01T10:00:00", "deadline", "2026-03-15T23:59:59"),
            java.util.Map.of("projectId", 102, "title", "안드로이드 앱 유지보수", "status", "진행중", "applicantCount", 12, "createdAt", "2026-02-15T09:00:00", "deadline", "2026-02-28T23:59:59")
        );
        redisTemplate.opsForValue().set(listKey, mockList);

        // 4. Employer Applicant Status Data
        String applicantsKey1 = "employer:project:applicants:101";
        java.util.List<java.util.Map<String, Object>> mockApplicants101 = java.util.List.of(
            java.util.Map.of("freelancerId", 1, "applyStatus", "검토중"),
            java.util.Map.of("freelancerId", 2, "applyStatus", "면접"),
            java.util.Map.of("freelancerId", 3, "applyStatus", "합격")
        );
        redisTemplate.opsForValue().set(applicantsKey1, mockApplicants101);

        String applicantsKey2 = "employer:project:applicants:102";
        java.util.List<java.util.Map<String, Object>> mockApplicants102 = java.util.List.of(
            java.util.Map.of("freelancerId", 4, "applyStatus", "검토중")
        );
        redisTemplate.opsForValue().set(applicantsKey2, mockApplicants102);

        // 5. Employer Subscription Data
        String subscriptionKey1 = "employer:subscription:1";
        java.util.Map<String, Object> mockSubscription1 = java.util.Map.of(
            "currentPlan", "PRIME",
            "features", java.util.List.of("인재풀 무제한 열람", "프로젝트 상단 노출", "수수료 면제", "전담 매니저 배정"),
            "nextBillingDate", "2026-04-03T12:00:00"
        );
        redisTemplate.opsForValue().set(subscriptionKey1, mockSubscription1);

        String subscriptionKey2 = "employer:subscription:2";
        java.util.Map<String, Object> mockSubscription2 = java.util.Map.of(
            "currentPlan", "BASIC",
            "features", java.util.List.of("기본 프로젝트 등록", "지원자 열람"),
            "nextBillingDate", "2026-03-15T12:00:00"
        );
        redisTemplate.opsForValue().set(subscriptionKey2, mockSubscription2);

        return "Successfully injected mock data for User ID 1 into Cloud Redis!";
    }
}
