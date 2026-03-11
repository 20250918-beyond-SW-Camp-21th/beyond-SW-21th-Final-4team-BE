package com.fallguys.mypage.api.web.dto.employer.response;

import java.time.LocalDateTime;

public record EmployerProjectListResponseDto(
        Long projectId,
        String title,
        String status, // 대기중, 모집중, 진행중 등
        Integer applicantCount,
        LocalDateTime createdAt,
        LocalDateTime deadline,
        String description,
        String monthlySalary
) {
    public static EmployerProjectListResponseDto from(java.util.Map<String, Object> data) {
        if (data == null || data.isEmpty()) {
            return null; // Return null effectively filtering it out during stream processing
        }
        String description = getString(data, "description", "detail", "projectDescription");
        String monthlySalary = getString(data, "monthlySalary", "salary", "budget");
        return new EmployerProjectListResponseDto(
                data.get("projectId") != null ? Long.valueOf(data.get("projectId").toString()) : null,
                data.get("title") != null ? data.get("title").toString() : null,
                data.get("status") != null ? data.get("status").toString() : null,
                data.get("applicantCount") != null ? Integer.valueOf(data.get("applicantCount").toString()) : 0,
                data.get("createdAt") != null ? LocalDateTime.parse(data.get("createdAt").toString()) : null,
                data.get("deadline") != null ? LocalDateTime.parse(data.get("deadline").toString()) : null,
                description,
                monthlySalary
        );
    }

    private static String getString(java.util.Map<String, Object> data, String... keys) {
        for (String key : keys) {
            Object value = data.get(key);
            if (value != null) {
                return value.toString();
            }
        }
        return null;
    }
}
