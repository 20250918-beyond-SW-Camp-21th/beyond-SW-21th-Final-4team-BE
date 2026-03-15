package com.fallguys.user.api.shared.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 마이페이지 등에서 사용하는 사용자 계정 정보 DTO (role 제외).
 */
@Getter
@Builder
public class ExternalUserMyInfoResponse {

    private Long id;
    private String email;
    private String name;
    private String phone;
    private LocalDateTime createdAt;
    private Boolean emailVerified;
    private Boolean termsAgreed;
    private Boolean privacyAgreed;
}
