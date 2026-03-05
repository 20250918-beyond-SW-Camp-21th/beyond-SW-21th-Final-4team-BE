package com.fallguys.user.service;

import com.fallguys.common.event.EmailVerifiedEvent;
import com.fallguys.user.api.web.dto.request.LoginRequestDto;
import com.fallguys.user.api.web.dto.request.PasswordUpdateRequest;
import com.fallguys.user.api.web.dto.request.EmailNotificationSettingDto;
import com.fallguys.user.api.web.dto.request.SignupRequestDto;
import com.fallguys.user.api.web.dto.response.LoginResponseDto;
import com.fallguys.user.api.web.dto.response.UserResponseDto;
import com.fallguys.user.entity.Role;
import com.fallguys.user.entity.User;
import com.fallguys.user.repository.UserRepository;
import com.fallguys.common.security.JwtTokenProvider;
import com.fallguys.mypage.repository.freelancer.FreelancerRepository;
import com.fallguys.mypage.repository.employer.EmployerRepository;
import com.fallguys.mypage.entity.freelancer.Freelancer;
import com.fallguys.mypage.entity.freelancer.FreelancerGrade;
import com.fallguys.mypage.entity.employer.Employer;
import com.fallguys.mypage.entity.employer.Subscription;
import com.fallguys.mypage.entity.employer.Scale;
import com.fallguys.mypage.entity.resume.Resume;
import org.springframework.data.redis.core.StringRedisTemplate;
import com.fallguys.mypage.repository.resume.ResumeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final FreelancerRepository freelancerRepository;
    private final EmployerRepository employerRepository;
    private final ResumeRepository resumeRepository;
    private final StringRedisTemplate redisTemplate;
    private final RedisTokenService redisTokenService;

    @Async
    @EventListener
    @Transactional
    public void handleEmailVerifiedEvent(EmailVerifiedEvent event) {
        // 회원가입 전 이메일 인증 시에는 아직 User가 없으므로 Exception이 발생하지 않도록 처리
        userRepository.findByEmail(event.email()).ifPresentOrElse(
                user -> {
                    user.verifyEmail();
                    log.info("이메일 인증 완료 처리 (기존 회원) - email: {}", maskEmail(event.email()));
                },
                () -> log.info("이메일 인증 완료 (신규 가입 대기) - email: {}", maskEmail(event.email())));
    }

    /**
     * 회원가입
     */
    @Transactional
    public UserResponseDto signup(SignupRequestDto request) {
        // 이메일 중복 검사
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("이미 사용 중인 이메일입니다.");
        }

        // Redis에서 이메일 인증 완료 증표 확인
        String verifiedKey = "email:verified:" + request.getEmail();
        String isVerified = redisTemplate.opsForValue().get(verifiedKey);

        if (!"true".equals(isVerified)) {
            throw new IllegalArgumentException("이메일 인증이 완료되지 않았습니다.");
        }

        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .name(request.getName())
                .role(request.getRole())
                .termsAgreed(request.getTermsAgreed())
                .privacyAgreed(request.getPrivacyAgreed())
                .build();
        user.verifyEmail(); // 가입 시 이메일 인증 완료 처리

        User savedUser;
        try {
            savedUser = userRepository.save(user);
        } catch (DataIntegrityViolationException e) {
            log.warn("회원가입 중복 충돌 - email: {}", maskEmail(request.getEmail()));
            throw new IllegalArgumentException("이미 사용 중인 이메일입니다.");
        }
        log.info("회원가입 완료 - userId: {}", savedUser.getId());

        // 역할에 맞는 빈 프로필 자동 생성
        if (Role.FREELANCER.equals(savedUser.getRole())) {
            Freelancer freelancer = Freelancer.create(savedUser.getId(), "미입력", FreelancerGrade.JUNIOR);
            Freelancer savedFreelancer = freelancerRepository.save(freelancer);
            log.info("프리랜서 빈 프로필 생성 완료 - userId: {}", savedUser.getId());
            if (resumeRepository.findByFreelancerId(savedFreelancer.getFreelancerId()).isEmpty()) {
                resumeRepository.save(new Resume(savedFreelancer.getFreelancerId()));
                log.info("프리랜서 기본 이력서 생성 완료 - freelancerId: {}", savedFreelancer.getFreelancerId());
            }
        } else if (Role.EMPLOYER.equals(savedUser.getRole())) {
            // 필수 뼈대값 대입 (가입 시 법인명은 유저 이름으로 임시 설정)
            Employer employer = Employer.create(savedUser.getId(), Subscription.BASIC, savedUser.getName(), Scale.S1_4);
            employerRepository.save(employer);
            log.info("고용주 빈 프로필 생성 완료 - userId: {}", savedUser.getId());
        }

        // 인증 증표 삭제 (재가입 등 방지)
        redisTemplate.delete(verifiedKey);

        return UserResponseDto.from(savedUser);
    }

    /**
     * 로그인
     */
    public LoginResponseDto login(LoginRequestDto request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("이메일 또는 비밀번호가 올바르지 않습니다."));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new IllegalArgumentException("이메일 또는 비밀번호가 올바르지 않습니다.");
        }

        if (!Boolean.TRUE.equals(user.getEmailVerified())) {
            throw new IllegalArgumentException("이메일 또는 비밀번호가 올바르지 않습니다.");
        }

        String grade = "";
        if (Role.FREELANCER.equals(user.getRole())) {
            grade = freelancerRepository.findByUserId(user.getId())
                    .map(Freelancer::getGrade)
                    .map(Enum::name)
                    .orElseThrow(() -> new IllegalStateException("프리랜서 등급 정보가 존재하지 않습니다."));
        } else if (Role.EMPLOYER.equals(user.getRole())) {
            grade = employerRepository.findByUserId(user.getId())
                    .map(com.fallguys.mypage.entity.employer.Employer::getScale)
                    .map(Enum::name)
                    .orElse("UNKNOWN");
        }

        String accessToken = jwtTokenProvider.generateToken(
                user.getId(), user.getEmail(), user.getRole().name(), user.getName(), grade);

        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getId());
        redisTokenService.saveRefreshToken(user.getId(), refreshToken, 1000L * 60 * 60 * 24 * 7);

        log.info("로그인 성공 - userId: {}", user.getId());

        return LoginResponseDto.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .user(UserResponseDto.from(user))
                .grade(grade)
                .build();
    }

    /**
     * 로그아웃
     */
    @Transactional
    public void logout(Long userId, String accessToken) {
        // 1. Refresh Token 삭제
        redisTokenService.deleteRefreshToken(userId);

        // 2. Access Token을 블랙리스트에 추가
        if (accessToken != null && accessToken.startsWith("Bearer ")) {
            accessToken = accessToken.substring(7);
        }

        try {
            java.util.Date expiration = jwtTokenProvider.getClaimsFromToken(accessToken).getExpiration();
            long remainingTime = expiration.getTime() - System.currentTimeMillis();
            if (remainingTime > 0) {
                redisTokenService.addToBlacklist(accessToken, remainingTime);
            }
        } catch (Exception e) {
            log.warn("이미 만료되었거나 유효하지 않은 Access Token입니다.", e);
        }
        log.info("로그아웃 완료 - userId: {}", userId);
    }

    /**
     * 토큰 재발급
     */
    @Transactional
    public LoginResponseDto refreshTokens(String incomingRefreshToken) {
        // 1. Validate refresh token
        if (!jwtTokenProvider.validateToken(incomingRefreshToken)) {
            throw new IllegalArgumentException("유효하지 않거나 만료된 Refresh Token입니다.");
        }

        // 2. Get user ID from token
        String userIdStr = jwtTokenProvider.getClaimsFromToken(incomingRefreshToken).getSubject();
        Long userId = Long.valueOf(userIdStr);

        // 3. Verify token matches the one in Redis
        String savedToken = redisTokenService.getRefreshToken(userId);
        if (savedToken == null || !savedToken.equals(incomingRefreshToken)) {
            throw new IllegalArgumentException("Refresh Token이 일치하지 않거나 로그아웃 되었습니다.");
        }

        // 4. Generate new tokens
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));

        String grade = "";
        if (Role.FREELANCER.equals(user.getRole())) {
            grade = freelancerRepository.findByUserId(user.getId())
                    .map(Freelancer::getGrade)
                    .map(Enum::name)
                    .orElse("UNKNOWN");
        } else if (Role.EMPLOYER.equals(user.getRole())) {
            grade = employerRepository.findByUserId(user.getId())
                    .map(com.fallguys.mypage.entity.employer.Employer::getScale)
                    .map(Enum::name)
                    .orElse("UNKNOWN");
        }

        String newAccessToken = jwtTokenProvider.generateToken(
                user.getId(), user.getEmail(), user.getRole().name(), user.getName(), grade);

        String newRefreshToken = jwtTokenProvider.generateRefreshToken(user.getId());
        redisTokenService.saveRefreshToken(user.getId(), newRefreshToken, 1000L * 60 * 60 * 24 * 7);

        return LoginResponseDto.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .user(UserResponseDto.from(user))
                .grade(grade)
                .build();
    }

    /**
     * 이메일 중복 확인
     */
    public boolean checkEmailDuplicate(String email) {
        return userRepository.existsByEmail(email);
    }

    /**
     * ID로 사용자 조회 (다른 모듈에서 사용)
     */
    public UserResponseDto findById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));
        return UserResponseDto.from(user);
    }

    /**
     * 이메일로 사용자 조회
     */
    public UserResponseDto findByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));
        return UserResponseDto.from(user);
    }

    /*
     * 이메일 인증 완료 처리
     */
    @Transactional
    public void verifyUserEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));
        user.verifyEmail();
        log.info("이메일 인증 완료 - email: {}", maskEmail(email));
    }

    /*
     * 이메일 마스킹 (예: test@gmail.com → t***t@gmail.com)
     */
    private String maskEmail(String email) {
        int atIndex = email.indexOf('@');
        if (atIndex <= 1)
            return "***" + email.substring(atIndex);
        String local = email.substring(0, atIndex);
        String domain = email.substring(atIndex);
        return local.charAt(0) + "***" + local.charAt(local.length() - 1) + domain;
    }

    /**
     * 비밀번호 변경
     */
    @Transactional
    public void updatePassword(Long userId, PasswordUpdateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new IllegalArgumentException("현재 비밀번호가 일치하지 않습니다.");
        }

        user.updatePassword(passwordEncoder.encode(request.getNewPassword()));
        log.info("비밀번호 변경 완료 - userId: {}", userId);
    }

    /**
     * 이메일 수신 동의 상태 변경
     */
    @Transactional
    public void updateEmailNotificationSetting(Long userId, EmailNotificationSettingDto dto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));

        user.updateEmailEnabled(dto.getEmailEnabled());
        log.info("이메일 수신 설정 변경 - userId: {}, emailEnabled: {}", userId, dto.getEmailEnabled());
    }
}
