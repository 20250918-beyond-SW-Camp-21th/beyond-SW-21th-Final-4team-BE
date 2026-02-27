package com.fallguys.mypage.api.web.dto.employer.request;

public record UpdatePasswordRequestDto(
        String currentPassword,
        String newPassword
) {}
