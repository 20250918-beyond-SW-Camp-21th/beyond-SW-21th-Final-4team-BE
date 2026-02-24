package com.fallguys.mypage.dto.employer.request;

public record UpdatePasswordRequestDto(
        String currentPassword,
        String newPassword
) {}
