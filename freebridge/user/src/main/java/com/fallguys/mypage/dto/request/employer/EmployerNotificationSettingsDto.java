package com.fallguys.mypage.dto.request.employer;

public record EmployerNotificationSettingsDto(
        Boolean emailEnabled,
        Boolean kakaoEnabled,
        Boolean smsEnabled
) {}
