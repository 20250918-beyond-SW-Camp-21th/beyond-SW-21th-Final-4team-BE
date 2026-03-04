package com.fallguys.user.api.web;

import com.fallguys.user.dto.LoginRequestDto;
import com.fallguys.user.dto.LoginResponseDto;
import com.fallguys.user.dto.PasswordUpdateRequest;
import com.fallguys.user.dto.EmailNotificationSettingDto;
import com.fallguys.user.dto.SignupRequestDto;
import com.fallguys.user.dto.UserResponseDto;
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
     * 이메일 중복 확인
     * GET /api/users/check-email?email=xxx
     */
    @GetMapping("/check-email")
    public ResponseEntity<Map<String, Object>> checkEmail(@RequestParam String email) {
        boolean exists = userService.checkEmailDuplicate(email);
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("exists", exists);
        response.put("available", !exists);
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
