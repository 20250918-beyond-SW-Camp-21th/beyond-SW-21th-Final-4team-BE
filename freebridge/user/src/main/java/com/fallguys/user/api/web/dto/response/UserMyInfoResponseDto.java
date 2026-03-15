package com.fallguys.user.api.web.dto.response;

import com.fallguys.user.entity.User;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * /api/users/getmyinfo 응답용 DTO (role 제외)
 */
@Getter
@Builder
public class UserMyInfoResponseDto {

    private Long id;
    private String email;
    private String name;
    private String phone;
    private LocalDateTime createdAt;
    private Boolean emailVerified;
    private Boolean termsAgreed;
    private Boolean privacyAgreed;

    public static UserMyInfoResponseDto from(User user) {
        return UserMyInfoResponseDto.builder()
                .id(user.getId())
                .email(user.getEmail())
                .name(user.getName())
                .phone(user.getPhone())
                .createdAt(user.getCreatedAt())
                .emailVerified(user.getEmailVerified())
                .termsAgreed(user.getTermsAgreed())
                .privacyAgreed(user.getPrivacyAgreed())
                .build();
    }
}
