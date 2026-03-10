package com.fallguys.user.api.web.dto.response;

import com.fallguys.user.entity.Role;
import com.fallguys.user.entity.User;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class UserResponseDto {

    private Long id;
    private String email;
    private String name;
    private String phone;
    private Role role;
    private Boolean termsAgreed;
    private Boolean privacyAgreed;
    private Boolean emailVerified;
    private LocalDateTime createdAt;

    /*
     * User Entity → ResponseDto 변환
     */
    public static UserResponseDto from(User user) {
        return UserResponseDto.builder()
                .id(user.getId())
                .email(user.getEmail())
                .name(user.getName())
                .phone(user.getPhone())
                .role(user.getRole())
                .termsAgreed(user.getTermsAgreed())
                .privacyAgreed(user.getPrivacyAgreed())
                .emailVerified(user.getEmailVerified())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
