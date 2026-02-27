package com.fallguys.mypage.api.web.employer;

import com.fallguys.common.response.ApiResponse;
import com.fallguys.mypage.api.web.dto.employer.response.EmployerApplicantStatusResponseDto;
import com.fallguys.mypage.api.web.dto.employer.response.EmployerProjectListResponseDto;
import com.fallguys.mypage.api.web.dto.employer.response.EmployerProjectStatsResponseDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Employer MyPage - Project", description = "고용주 마이페이지 프로젝트 및 지원자 관리 API")
@RestController
@RequestMapping("/api/employer/mypage/projects")
@RequiredArgsConstructor
public class EmployerProjectController {

    @Operation(summary = "내 프로젝트 통계 조회", description = "누적 프로젝트 수, 현재 지원자 수 등의 통계를 조회합니다.")
    @GetMapping("/stats")
    public ApiResponse<EmployerProjectStatsResponseDto> getProjectStats(@RequestHeader("X-User-Id") String userId) {
        return ApiResponse.ok(null);
    }

    @Operation(summary = "내 프로젝트 목록 조회", description = "마이페이지에서 노출할 내 프로젝트 리스트를 조회합니다.")
    @GetMapping
    public ApiResponse<List<EmployerProjectListResponseDto>> getMyProjects(@RequestHeader("X-User-Id") String userId,
                                                                           @RequestParam(required = false) String status) {
        return ApiResponse.ok(null);
    }

    @Operation(summary = "특정 프로젝트 지원자 현황 조회", description = "해당 프로젝트에 지원한 프리랜서들의 상태를 조회합니다.")
    @GetMapping("/{projectId}/applicants/status")
    public ApiResponse<List<EmployerApplicantStatusResponseDto>> getApplicantStatus(@RequestHeader("X-User-Id") String userId,
                                                                                    @PathVariable Long projectId) {
        return ApiResponse.ok(null);
    }
}
