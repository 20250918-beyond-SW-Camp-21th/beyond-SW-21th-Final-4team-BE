package com.fallguys.mypage.api.web.freelancer;

import com.fallguys.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "Freelancer MyPage - Project", description = "프리랜서 프로젝트 관리 API")
@RestController
@RequestMapping("/api/mypage/freelancer/projects")
@RequiredArgsConstructor
public class FreelancerProjectController {

    @Operation(summary = "프로젝트 목록 조회", description = "프리랜서의 프로젝트 목록을 상태별로 조회합니다.")
    @GetMapping
    public ApiResponse<List<Map<String, Object>>> getProjects(@RequestHeader("X-User-Id") String userId,
                                                              @RequestParam(required = false) String status) {
        // TODO: 프로젝트 목록 조회 로직 구현
        // FreelancerProject DTO 리스트 반환
        return ApiResponse.ok(null);
    }

    @Operation(summary = "프로젝트 상세 조회", description = "특정 프로젝트의 상세 정보를 조회합니다.")
    @GetMapping("/{projectId}")
    public ApiResponse<Map<String, Object>> getProjectDetail(@RequestHeader("X-User-Id") String userId,
                                                             @PathVariable Long projectId) {
        // TODO: 프로젝트 상세 조회 로직 구현
        // 계약 정보, 결제 내역, 평가 등 포함
        return ApiResponse.ok(null);
    }
}
