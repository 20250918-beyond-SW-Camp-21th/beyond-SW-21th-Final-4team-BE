package com.fallguys.email.api.web;

import com.fallguys.email.api.web.dto.request.EmailVerificationRequestDto;
import com.fallguys.email.api.web.dto.request.EmailVerifyCodeRequestDto;
import com.fallguys.email.service.EmailVerificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class EmailController {

    private final EmailVerificationService emailVerificationService;

    /*
     * 이메일 인증코드 발송
     * POST /api/auth/send-verification
     */
    @PostMapping("/send-verification")
    public ResponseEntity<Map<String, Object>> sendVerificationCode(
            @Valid @RequestBody EmailVerificationRequestDto request) {
        try {
            emailVerificationService.sendVerificationCode(request.getEmail());
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "인증코드가 이메일로 발송되었습니다.",
                    "data", Map.of()));
        } catch (Exception e) {
            log.error("이메일 발송 실패: {}", request.getEmail(), e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "message", "이메일 발송에 실패했습니다."));
        }
    }

    /*
     * 이메일 인증코드 확인
     * POST /api/auth/verify-email
     */
    @PostMapping("/verify-email")
    public ResponseEntity<Map<String, Object>> verifyEmail(
            @Valid @RequestBody EmailVerifyCodeRequestDto request) {
        boolean verified = emailVerificationService.verifyCode(
                request.getEmail(), request.getCode());

        if (verified) {
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "이메일 인증이 완료되었습니다.",
                    "data", Map.of()));
        } else {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "인증코드가 올바르지 않거나 만료되었습니다."));
        }
    }

    /*
     * 인증코드 재발송
     * POST /api/auth/resend-verification
     */
    @PostMapping("/resend-verification")
    public ResponseEntity<Map<String, Object>> resendVerificationCode(
            @Valid @RequestBody EmailVerificationRequestDto request) {
        try {
            emailVerificationService.sendVerificationCode(request.getEmail());
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "인증코드가 재발송되었습니다.",
                    "data", Map.of()));
        } catch (Exception e) {
            log.error("이메일 재발송 실패: {}", request.getEmail(), e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "message", "이메일 재발송에 실패했습니다."));
        }
    }
}
