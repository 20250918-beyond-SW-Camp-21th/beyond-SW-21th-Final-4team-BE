package com.fallguys.user.controller;

import com.fallguys.user.dto.LoginRequestDto;
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
            UserResponseDto user = userService.login(request);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "로그인 성공");
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
}
