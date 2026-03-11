package com.fallguys.user.service;

import com.fallguys.common.exception.BusinessException;
import com.fallguys.common.exception.ErrorCode;
import com.fallguys.user.api.shared.ExternalUserApi;
import com.fallguys.user.api.shared.response.ExternalUserMyInfoResponse;
import com.fallguys.user.api.shared.response.ExternalUserResponse;
import com.fallguys.user.entity.User;
import com.fallguys.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ExternalUserApiImpl implements ExternalUserApi {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public ExternalUserResponse getUserById(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        return toSharedDto(user);
    }

    @Override
    public ExternalUserResponse getUserByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다. Email: " + email));
        return toSharedDto(user);
    }

    @Override
    public ExternalUserMyInfoResponse getMyInfo(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        return toMyInfoDto(user);
    }

    @Override
    public boolean existsById(Long userId) {
        return userRepository.existsById(userId);
    }

    @Override
    @Transactional
    public void updatePassword(Long userId, String currentPassword, String newPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다. ID: " + userId));

        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new IllegalArgumentException("현재 비밀번호가 일치하지 않습니다.");
        }

        // 새로운 비밀번호 유효성 검사
        validatePassword(newPassword);

        user.updatePassword(passwordEncoder.encode(newPassword));
    }

    @Override
    @Transactional
    public void updateEmailNotificationSetting(Long userId, boolean emailEnabled) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다. ID: " + userId));
        user.updateEmailEnabled(emailEnabled);
    }

    @Override
    @Transactional
    public void updateName(Long userId, String name) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        user.updateName(name);
    }

    @Override
    @Transactional
    public void updatePhone(Long userId, String phone) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        user.updatePhone(phone);
    }

    /*
     * 내부 User Entity를 외부 공유 DTO(ExternalUserResponse)로 변환
     */
    private ExternalUserResponse toSharedDto(User user) {
        return ExternalUserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .name(user.getName())
                // Enum 타입을 String으로 표현해 외 모듈에서의 직렬화 문제 방지
                .role(user.getRole() != null ? user.getRole().name() : null)
                .emailVerified(user.getEmailVerified())
                .emailEnabled(user.getEmailEnabled())
                .createdAt(user.getCreatedAt())
                .build();
    }

    private ExternalUserMyInfoResponse toMyInfoDto(User user) {
        return ExternalUserMyInfoResponse.builder()
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

    /**
     * 비밀번호 유효성 검사 (대문자, 소문자, 숫자, 특수문자 포함 8자 이상)
     */
    private void validatePassword(String password) {
        String regex = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*#?&])[A-Za-z\\d@$!%*#?&]{8,}$";
        if (password == null || !password.matches(regex)) {
            throw new IllegalArgumentException("비밀번호는 대문자, 소문자, 숫자, 특수문자를 포함하여 8자 이상이어야 합니다.");
        }
    }
}
