package com.fallguys.mypage.api.web.employer;

import com.fallguys.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "Employer MyPage - Account", description = "고용주 계정 관리 API")
@RestController
@RequestMapping("/api/mypage/employer/account")
@RequiredArgsConstructor
public class EmployerAccountController {

    @Operation(summary = "계정 정보 수정", description = "고용주 계정 정보(담당자 연락처 등)를 수정합니다.")
    @PutMapping
    public ApiResponse<Void> updateAccountInfo(@RequestHeader("X-User-Id") String userId,
                                               @RequestBody Map<String, Object> accountInfo) {
        // TODO: 계정 정보 수정 로직 구현
        return ApiResponse.ok(null);
    }
}
