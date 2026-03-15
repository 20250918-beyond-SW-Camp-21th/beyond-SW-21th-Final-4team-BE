package com.fallguys.mypage.api.web.freelancer;

import com.fallguys.common.response.ApiResponse;
import com.fallguys.common.security.CustomUserDetails;
import com.fallguys.mypage.api.web.dto.freelancer.response.FreelancerProjectListDto;
import com.fallguys.mypage.api.web.dto.freelancer.response.FreelancerProjectStatusStatsDto;
import com.fallguys.mypage.service.freelancer.FreelancerProjectService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "7. Freelancer MyPage - Project", description = "프리랜서 프로젝트 지원 및 진행현황 관리")
@RestController
@RequestMapping("/api/freelancer/mypage/projects")
@RequiredArgsConstructor
public class FreelancerProjectController {

    private final FreelancerProjectService freelancerProjectService;

    @Operation(summary = "상태별 프로젝트 통계", description = "지원/진행/완료된 프로젝트의 건수를 조회합니다.")
    @GetMapping("/stats")
    public ApiResponse<FreelancerProjectStatusStatsDto> getProjectStats(@AuthenticationPrincipal CustomUserDetails userDetails) {
        return ApiResponse.ok(freelancerProjectService.getProjectStats(userDetails.getId()));
    }

    @Operation(summary = "내 프로젝트 목록", description = "상태값을 받아(status 파라미터) 해당 프로젝트 목록을 조회합니다.")
    @GetMapping
    public ApiResponse<List<FreelancerProjectListDto>> getMyProjects(@AuthenticationPrincipal CustomUserDetails userDetails,
                                                                     @RequestParam(required = false) String status) {
        return ApiResponse.ok(freelancerProjectService.getMyProjects(userDetails.getId(), status));
    }
}
