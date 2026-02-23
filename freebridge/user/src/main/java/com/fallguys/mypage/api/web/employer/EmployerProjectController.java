package com.fallguys.mypage.api.web.employer;

import com.fallguys.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "Employer MyPage - Project/Application", description = "고용주 프로젝트 및 지원자 관리 API")
@RestController
@RequestMapping("/api/mypage/employer/projects")
@RequiredArgsConstructor
public class EmployerProjectController {

    @Operation(summary = "프로젝트별 지원자 목록 조회", description = "고용주가 등록한 프로젝트별로 지원자 목록을 조회합니다.")
    @GetMapping("/applications")
    public ApiResponse<List<Map<String, Object>>> getApplications(@RequestHeader("X-User-Id") String userId) {
        // TODO: 지원자 목록 조회 로직 구현
        // ApplicationGroup DTO 반환
        return ApiResponse.ok(null);
    }

    @Operation(summary = "지원자 합격 처리", description = "특정 지원자를 합격 처리합니다.")
    @PostMapping("/applications/{applicationId}/accept")
    public ApiResponse<Void> acceptApplication(@RequestHeader("X-User-Id") String userId,
                                               @PathVariable String applicationId) {
        // TODO: 합격 처리 로직 구현
        return ApiResponse.ok(null);
    }

    @Operation(summary = "지원자 불합격 처리", description = "특정 지원자를 불합격 처리하고 사유를 저장합니다.")
    @PostMapping("/applications/{applicationId}/reject")
    public ApiResponse<Void> rejectApplication(@RequestHeader("X-User-Id") String userId,
                                               @PathVariable String applicationId,
                                               @RequestBody Map<String, String> request) {
        // TODO: 불합격 처리 및 사유 저장 로직 구현
        // Request: { reason: "..." }
        return ApiResponse.ok(null);
    }
}
