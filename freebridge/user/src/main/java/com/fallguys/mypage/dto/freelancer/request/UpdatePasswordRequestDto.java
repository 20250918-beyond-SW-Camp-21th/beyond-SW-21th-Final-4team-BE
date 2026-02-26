package com.fallguys.mypage.dto.freelancer.request;

public record UpdatePasswordRequestDto(
        String currentPassword,
        String newPassword
) {}
