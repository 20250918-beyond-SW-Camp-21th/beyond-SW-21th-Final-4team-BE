package com.fallguys.mypage.api.web.dto.employer.request;

import jakarta.validation.constraints.NotNull;

public record UpdateNotificationSettingsRequestDto(
        @NotNull(message = "이메일 알림 수신 동의 여부는 필수 입력값입니다.")
        Boolean emailEnabled
) {}
