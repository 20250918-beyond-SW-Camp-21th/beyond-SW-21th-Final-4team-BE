package com.fallguys.user.service;

import com.fallguys.user.api.web.dto.request.SignupRequestDto;
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

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;

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

    @Test
    @DisplayName("비밀번호 검증 테스트 - 유효하지 않은 형식 (8자 미만)")
    void validatePassword_TooShort() {
        PasswordUpdateRequest request = PasswordUpdateRequest.builder()
                .currentPassword("oldPassword123!")
                .newPassword("Short1!")
                .build();

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            userService.updatePassword(1L, request);
        });

        assertEquals("비밀번호는 영문, 숫자, 특수문자를 포함하여 8자 이상이어야 합니다.", exception.getMessage());
    }

    @Test
    @DisplayName("비밀번호 검증 테스트 - 유효하지 않은 형식 (특수문자 없음)")
    void validatePassword_NoSpecialChar() {
        PasswordUpdateRequest request = PasswordUpdateRequest.builder()
                .currentPassword("oldPassword123!")
                .newPassword("Password123")
                .build();

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            userService.updatePassword(1L, request);
        });

        assertEquals("비밀번호는 영문, 숫자, 특수문자를 포함하여 8자 이상이어야 합니다.", exception.getMessage());
    }

    @Test
    @DisplayName("비밀번호 검증 테스트 - 유효하지 않은 형식 (숫자 없음)")
    void validatePassword_NoDigit() {
        PasswordUpdateRequest request = PasswordUpdateRequest.builder()
                .currentPassword("oldPassword123!")
                .newPassword("Password@")
                .build();

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            userService.updatePassword(1L, request);
        });

        assertEquals("비밀번호는 영문, 숫자, 특수문자를 포함하여 8자 이상이어야 합니다.", exception.getMessage());
    }
}
