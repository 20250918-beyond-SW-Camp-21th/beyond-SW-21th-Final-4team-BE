package com.fallguys.mypage.dto.employer.response;

public record EmployerNotificationSettingsDto(
        Boolean emailEnabled,
        Boolean kakaoEnabled,
        Boolean smsEnabled
) {}
