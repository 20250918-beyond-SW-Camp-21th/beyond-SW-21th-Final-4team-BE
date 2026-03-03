package com.fallguys.user.api.shared.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 타 모듈(예: chatting, review 등)로 전달하기 위한 User 데이터 DTO입니다.
 * user 모듈 내부의 엔티티나 로직에 의존성을 띠지 않도록 별도로 정의합니다.
 */
@Getter
@Builder
public class ExternalUserResponse {

    private Long id;
    private String email;
    private String name;
    private String role; // Enum 대신 String 사용하여 강한 결합 제거
    private Boolean emailVerified;
    private Boolean emailEnabled;
    private LocalDateTime createdAt;
}
