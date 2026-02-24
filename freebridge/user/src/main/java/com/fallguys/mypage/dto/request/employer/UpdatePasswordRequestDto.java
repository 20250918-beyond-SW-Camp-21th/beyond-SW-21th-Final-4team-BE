package com.fallguys.mypage.dto.request.employer;

public record UpdatePasswordRequestDto(
        String currentPassword,
        String newPassword
) {}
