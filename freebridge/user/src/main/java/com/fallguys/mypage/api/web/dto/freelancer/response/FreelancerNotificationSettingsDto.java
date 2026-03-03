package com.fallguys.mypage.api.web.dto.freelancer.response;

public record FreelancerNotificationSettingsDto(
        Boolean requestNotificationEnabled, // 프로젝트 제안 알림
        Boolean contractNotificationEnabled // 계약 상태 변경 알림
) {}
