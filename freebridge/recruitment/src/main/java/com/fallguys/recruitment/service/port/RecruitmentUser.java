package com.fallguys.recruitment.service.port;

public record RecruitmentUser(
        Long id,
        String name,
        String skills,
        String experience,
        String status
) {
}
