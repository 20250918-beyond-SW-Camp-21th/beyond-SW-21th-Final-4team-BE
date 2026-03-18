package com.fallguys.user.service;

import com.fallguys.common.ai.port.RecommendationEngine;
import org.springframework.context.ApplicationEventPublisher;
import com.fallguys.user.api.web.dto.request.PasswordUpdateRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.fallguys.user.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import com.fallguys.common.security.JwtTokenProvider;
import com.fallguys.mypage.repository.freelancer.FreelancerRepository;
import com.fallguys.mypage.repository.employer.EmployerRepository;
import com.fallguys.mypage.repository.resume.ResumeRepository;
import org.springframework.data.redis.core.StringRedisTemplate;
import com.fallguys.user.entity.Role;
import com.fallguys.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class UserServiceValidationTest {

    @InjectMocks
    private UserService userService;

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtTokenProvider jwtTokenProvider;
    @Mock
    private FreelancerRepository freelancerRepository;
    @Mock
    private EmployerRepository employerRepository;
    @Mock
    private ResumeRepository resumeRepository;
    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private RedisTokenService redisTokenService;
    @Mock
    private RecommendationEngine recommendationEngine;
    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    @BeforeEach
    void setUp() {
        User user = User.builder()
                .email("test@example.com")
                .password("encodedPassword")
                .name("Test User")
                .role(Role.FREELANCER)
                .termsAgreed(true)
                .privacyAgreed(true)
                .build();

        lenient().when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        lenient().when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);
    }

    @Test
    @DisplayName("비밀번호 검증 테스트 - 유효하지 않은 형식 (8자 미만)")
    void validatePassword_TooShort() {
        PasswordUpdateRequest request = PasswordUpdateRequest.builder()
                .currentPassword("oldPassword123!")
                .newPassword("Short1!")
                .build();

        assertThrows(IllegalArgumentException.class, () -> {
            userService.updatePassword(1L, request);
        });
    }

    @Test
    @DisplayName("비밀번호 검증 테스트 - 유효하지 않은 형식 (특수문자 없음)")
    void validatePassword_NoSpecialChar() {
        PasswordUpdateRequest request = PasswordUpdateRequest.builder()
                .currentPassword("oldPassword123!")
                .newPassword("Password123")
                .build();

        assertThrows(IllegalArgumentException.class, () -> {
            userService.updatePassword(1L, request);
        });
    }

    @Test
    @DisplayName("비밀번호 검증 테스트 - 유효하지 않은 형식 (숫자 없음)")
    void validatePassword_NoDigit() {
        PasswordUpdateRequest request = PasswordUpdateRequest.builder()
                .currentPassword("oldPassword123!")
                .newPassword("Password@")
                .build();

        assertThrows(IllegalArgumentException.class, () -> {
            userService.updatePassword(1L, request);
        });
    }

    @Test
    @DisplayName("비밀번호 검증 테스트 - 유효하지 않은 형식 (대문자 없음)")
    void validatePassword_NoUpperCase() {
        PasswordUpdateRequest request = PasswordUpdateRequest.builder()
                .currentPassword("oldPassword123!")
                .newPassword("password123!")
                .build();

        assertThrows(IllegalArgumentException.class, () -> {
            userService.updatePassword(1L, request);
        });
    }

    @Test
    @DisplayName("비밀번호 검증 테스트 - 유효하지 않은 형식 (소문자 없음)")
    void validatePassword_NoLowerCase() {
        PasswordUpdateRequest request = PasswordUpdateRequest.builder()
                .currentPassword("oldPassword123!")
                .newPassword("PASSWORD123!")
                .build();

        assertThrows(IllegalArgumentException.class, () -> {
            userService.updatePassword(1L, request);
        });
    }
}
