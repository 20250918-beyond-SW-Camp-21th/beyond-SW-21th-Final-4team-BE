package com.fallguys.mypage.api.web.freelancer;

import com.fallguys.common.response.ApiResponse;
import com.fallguys.mypage.api.web.dto.freelancer.response.FreelancerAppliedProjectListDto;
import com.fallguys.mypage.api.web.dto.freelancer.response.FreelancerProjectStatusStatsDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "7. Freelancer MyPage - Project", description = "프리랜서 프로젝트 지원 및 진행현황 관리")
@RestController
@RequestMapping("/api/freelancer/mypage/projects")
@RequiredArgsConstructor
public class FreelancerProjectController {

    @Operation(summary = "상태별 프로젝트 통계", description = "지원/진행/완료된 프로젝트의 건수를 조회합니다.")
    @GetMapping("/stats")
    public ApiResponse<FreelancerProjectStatusStatsDto> getProjectStats(@RequestHeader("X-User-Id") String userId) {
        return ApiResponse.ok(null);
    }

    @Operation(summary = "내 지원 및 진행 프로젝트 목록", description = "상태값을 받아(status파라미터 등) 해당 프로젝트 목록을 조회합니다.")
    @GetMapping
    public ApiResponse<List<FreelancerAppliedProjectListDto>> getMyProjects(@RequestHeader("X-User-Id") String userId,
                                                                            @RequestParam(required = false) String status) {
        return ApiResponse.ok(null);
    }
}
