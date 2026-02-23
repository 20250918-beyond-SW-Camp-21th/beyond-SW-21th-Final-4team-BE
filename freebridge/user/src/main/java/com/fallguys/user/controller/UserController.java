package com.fallguys.user.controller;

import com.fallguys.user.dto.LoginRequestDto;
import com.fallguys.user.dto.SignupRequestDto;
import com.fallguys.user.dto.UserResponseDto;
import com.fallguys.user.config.JwtTokenProvider;
import com.fallguys.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final JwtTokenProvider jwtTokenProvider;

    /**
     * 회원가입
     * POST /api/users/signup
     */
    @PostMapping("/signup")
    public ResponseEntity<Map<String, Object>> signup(@Valid @RequestBody SignupRequestDto request) {
        try {
            UserResponseDto user = userService.signup(request);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "회원가입이 완료되었습니다.",
                    "data", user));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()));
        }
    }

    /**
     * 로그인
     * POST /api/users/login
     */
    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@Valid @RequestBody LoginRequestDto request) {
        try {
            UserResponseDto user = userService.login(request);
            String token = jwtTokenProvider.createToken(
                    user.getId(), user.getEmail(), user.getRole().name());
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "로그인 성공",
                    "token", token,
                    "data", user));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()));
        }
    }

    /**
     * 이메일 중복 확인
     * GET /api/users/check-email?email=xxx
     */
    @GetMapping("/check-email")
    public ResponseEntity<Map<String, Object>> checkEmail(@RequestParam String email) {
        boolean exists = userService.checkEmailDuplicate(email);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "exists", exists,
                "available", !exists));
    }

    /**
     * ID로 사용자 조회 (인증 필요)
     * GET /api/users/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getUserById(
            @PathVariable Long id,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        if (!isAuthenticated(authHeader)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                    "success", false,
                    "message", "인증이 필요합니다."));
        }
        try {
            UserResponseDto user = userService.findById(id);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", user));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()));
        }
    }

    /**
     * 이메일로 사용자 조회 (인증 필요)
     * GET /api/users/by-email?email=xxx
     */
    @GetMapping("/by-email")
    public ResponseEntity<Map<String, Object>> getUserByEmail(
            @RequestParam String email,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        if (!isAuthenticated(authHeader)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                    "success", false,
                    "message", "인증이 필요합니다."));
        }
        try {
            UserResponseDto user = userService.findByEmail(email);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", user));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()));
        }
    }

    /**
     * Authorization 헤더에서 Bearer 토큰 추출 및 유효성 검증
     */
    private boolean isAuthenticated(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return false;
        }
        String token = authHeader.substring(7);
        return jwtTokenProvider.validateToken(token);
    }
}
