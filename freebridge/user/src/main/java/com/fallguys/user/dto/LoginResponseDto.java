package com.fallguys.user.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class LoginResponseDto {
    private String accessToken;
    private UserResponseDto user;
    private String grade; // FREELANCER인 경우만 값 세팅 (JUNIOR 등)
}
