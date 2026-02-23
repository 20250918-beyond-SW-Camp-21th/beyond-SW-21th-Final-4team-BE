package com.fallguys.mypage.api.web.freelancer;

import com.fallguys.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "Freelancer MyPage - Account", description = "프리랜서 계정 관리 API")
@RestController
@RequestMapping("/api/mypage/freelancer/account")
@RequiredArgsConstructor
public class FreelancerAccountController {

    @Operation(summary = "계정 정보 수정", description = "이메일, 연락처 등 계정 기본 정보를 수정합니다.")
    @PutMapping
    public ApiResponse<Void> updateAccountInfo(@RequestHeader("X-User-Id") String userId,
                                               @RequestBody Map<String, Object> accountInfo) {
        // TODO: 계정 정보 수정 로직 구현
        // AccountInfo { email, name, phone ... }
        return ApiResponse.ok(null);
    }

    @Operation(summary = "비밀번호 변경", description = "계정 비밀번호를 변경합니다.")
    @PostMapping("/password")
    public ApiResponse<Void> changePassword(@RequestHeader("X-User-Id") String userId,
                                            @RequestBody Map<String, String> passwordRequest) {
        // TODO: 비밀번호 변경 로직 구현
        // Request: { current, new, confirm }
        return ApiResponse.ok(null);
    }

    @Operation(summary = "회원 탈퇴", description = "프리랜서 회원을 탈퇴 처리합니다.")
    @DeleteMapping
    public ApiResponse<Void> deleteAccount(@RequestHeader("X-User-Id") String userId) {
        // TODO: 회원 탈퇴 로직 구현
        return ApiResponse.ok(null);
    }
}
