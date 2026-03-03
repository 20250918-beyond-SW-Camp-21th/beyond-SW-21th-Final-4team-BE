package com.fallguys.user.service;

import com.fallguys.user.api.shared.ExternalUserApi;
import com.fallguys.user.api.shared.response.ExternalUserResponse;
import com.fallguys.user.entity.User;
import com.fallguys.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ExternalUserApiImpl implements ExternalUserApi {

    private final UserRepository userRepository;

    @Override
    public ExternalUserResponse getUserById(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다. ID: " + userId));
        return toSharedDto(user);
    }

    @Override
    public ExternalUserResponse getUserByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다. Email: " + email));
        return toSharedDto(user);
    }

    @Override
    public boolean existsById(Long userId) {
        return userRepository.existsById(userId);
    }

    @Override
    @Transactional
    public void updatePassword(Long userId, String newPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다. ID: " + userId));
        user.updatePassword(newPassword);
    }

    @Override
    @Transactional
    public void updateEmailNotificationSetting(Long userId, boolean emailEnabled) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다. ID: " + userId));
        user.updateEmailEnabled(emailEnabled);
    }

    /*
     * 내부 User Entity를 외부용 공유 DTO(ExternalUserResponse)로 변환
     */
    private ExternalUserResponse toSharedDto(User user) {
        return ExternalUserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .name(user.getName())
                // Enum 대신 String으로 전달하여 타 모듈에서의 역직렬화 및 강결합 문제 방지
                .role(user.getRole() != null ? user.getRole().name() : null)
                .emailVerified(user.getEmailVerified())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
