package com.fallguys.user.api.web;

import com.fallguys.user.api.web.dto.request.LoginRequestDto;
import com.fallguys.user.api.web.dto.response.LoginResponseDto;
import com.fallguys.user.api.web.dto.request.PasswordUpdateRequest;
import com.fallguys.user.api.web.dto.request.EmailNotificationSettingDto;
import com.fallguys.user.api.web.dto.request.SignupRequestDto;
import com.fallguys.user.api.web.dto.response.UserMyInfoResponseDto;
import com.fallguys.user.api.web.dto.response.UserResponseDto;
import com.fallguys.user.api.web.dto.request.RefreshTokenRequestDto;
import com.fallguys.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Validated
public class UserController {

    private final UserService userService;

    /*
     * 회원가입
     * POST /api/users/signup
     */
    @PostMapping("/signup")
    public ResponseEntity<Map<String, Object>> signup(@Valid @RequestBody SignupRequestDto request) {
        try {
            UserResponseDto user = userService.signup(request);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "회원가입이 완료되었습니다.");
            response.put("data", user);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /*
     * 로그인
     * POST /api/users/login
     */
    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@Valid @RequestBody LoginRequestDto request) {
        try {
            LoginResponseDto loginResponse = userService.login(request);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "로그인 성공");
            response.put("data", loginResponse);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /*
     * 로그아웃
     * POST /api/users/logout
     */
    @PostMapping("/logout")
    public ResponseEntity<Map<String, Object>> logout(
            @org.springframework.security.core.annotation.AuthenticationPrincipal com.fallguys.common.security.CustomUserDetails userDetails,
            jakarta.servlet.http.HttpServletRequest request) {

        if (userDetails == null) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "인증 정보가 없습니다.");
            return ResponseEntity.status(401).body(response);
        }

        String authHeader = request.getHeader("Authorization");

        try {
            userService.logout(userDetails.getId(), authHeader);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "로그아웃 성공");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /*
     * 토큰 재발급
     * POST /api/users/refresh
     */
    @PostMapping("/refresh")
    public ResponseEntity<Map<String, Object>> refreshTokens(
            @Valid @RequestBody RefreshTokenRequestDto request) {
        try {
            LoginResponseDto tokens = userService
                    .refreshTokens(request.getRefreshToken());
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "토큰 재발급 성공");
            response.put("data", tokens);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(401).body(response);
        }
    }

    /*
     * 이메일 중복 확인
     * GET /api/users/check-email?email=xxx
     */
    @GetMapping("/check-email")
    public ResponseEntity<Map<String, Object>> checkEmail(@RequestParam String email) {
        boolean exists = userService.checkEmailDuplicate(email);
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);

        Map<String, Object> data = new HashMap<>();
        data.put("exists", exists);
        data.put("available", !exists);
        response.put("data", data);

        return ResponseEntity.ok(response);
    }

    /*
     * ID로 사용자 조회
     * GET /api/users/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getUserById(@PathVariable Long id) {
        try {
            UserResponseDto user = userService.findById(id);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", user);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /*
     * 이메일로 사용자 조회
     * GET /api/users/by-email?email=xxx
     */
    @GetMapping("/by-email")
    public ResponseEntity<Map<String, Object>> getUserByEmail(@RequestParam String email) {
        try {
            UserResponseDto user = userService.findByEmail(email);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", user);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /*
     * JWT 필터 해독 엔드포인트
     * GET /api/users/me/test
     */
    @GetMapping("/me/test")
    public ResponseEntity<Map<String, Object>> testJwtFilter(
            @org.springframework.security.core.annotation.AuthenticationPrincipal com.fallguys.common.security.CustomUserDetails user) {

        Map<String, Object> response = new HashMap<>();
        if (user == null) {
            response.put("success", false);
            response.put("message", "인증 정보가 없습니다. (토큰 없음 또는 만료)");
            return ResponseEntity.status(401).body(response);
        }

        response.put("success", true);
        response.put("message", "JWT 필터 해독 성공!");

        // CustomUserDetails 안에 있는 모든 데이터를 보여줌
        Map<String, Object> userData = new HashMap<>();
        userData.put("id", user.getId());
        userData.put("email", user.getEmail());
        userData.put("name", user.getName());
        userData.put("role", user.getRole());
        userData.put("grade", user.getGrade());

        response.put("data", userData);

        return ResponseEntity.ok(response);
    }

    /*
     * 마이페이지용 내 계정 정보 조회
     * GET /api/users/getmyinfo
     */
    @GetMapping("/getmyinfo")
    public ResponseEntity<Map<String, Object>> getMyInfo(
            @org.springframework.security.core.annotation.AuthenticationPrincipal com.fallguys.common.security.CustomUserDetails user) {
        Map<String, Object> response = new HashMap<>();
        if (user == null) {
            response.put("success", false);
            response.put("message", "인증 정보가 없습니다.");
            return ResponseEntity.status(401).body(response);
        }

        try {
            UserMyInfoResponseDto data = userService.getMyInfo(user.getId());
            response.put("success", true);
            response.put("data", data);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(org.springframework.http.HttpStatus.NOT_FOUND).body(response);
        }
    }

    /*
     * Update my account info
     * PUT /api/users/me/info
     */
    @PutMapping("/me/info")
    public ResponseEntity<Map<String, Object>> updateMyInfo(
            @org.springframework.security.core.annotation.AuthenticationPrincipal com.fallguys.common.security.CustomUserDetails user,
            @RequestBody com.fallguys.user.api.web.dto.request.AccountInfoUpdateRequest request) {
        Map<String, Object> response = new HashMap<>();
        if (user == null) {
            response.put("success", false);
            response.put("message", "Unauthorized.");
            return ResponseEntity.status(401).body(response);
        }
        try {
            UserMyInfoResponseDto data = userService.updateAccountInfo(user.getId(), request);
            response.put("success", true);
            response.put("data", data);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /*
     * 비밀번호 변경
     * PUT /api/users/me/password
     */
    @PutMapping("/me/password")
    public ResponseEntity<Map<String, Object>> updatePassword(
            @org.springframework.security.core.annotation.AuthenticationPrincipal com.fallguys.common.security.CustomUserDetails userDetails,
            @Valid @RequestBody PasswordUpdateRequest request) {

        if (userDetails == null) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "인증 정보가 없습니다.");
            return ResponseEntity.status(401).body(response);
        }

        try {
            userService.updatePassword(userDetails.getId(), request);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "비밀번호가 성공적으로 변경되었습니다.");
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /*
     * 이메일 알림 수신 상태 변경
     * PATCH /api/users/me/notifications/email
     */
    @PatchMapping("/me/notifications/email")
    public ResponseEntity<Map<String, Object>> updateEmailNotificationSetting(
            @org.springframework.security.core.annotation.AuthenticationPrincipal com.fallguys.common.security.CustomUserDetails userDetails,
            @Valid @RequestBody EmailNotificationSettingDto request) {

        if (userDetails == null) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "인증 정보가 없습니다.");
            return ResponseEntity.status(401).body(response);
        }

        try {
            userService.updateEmailNotificationSetting(userDetails.getId(), request);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "이메일 알림 설정이 성공적으로 변경되었습니다.");
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
}
