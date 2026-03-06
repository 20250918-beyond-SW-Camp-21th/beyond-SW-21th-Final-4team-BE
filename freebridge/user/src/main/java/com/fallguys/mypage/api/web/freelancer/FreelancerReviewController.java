package com.fallguys.mypage.api.web.freelancer;

import com.fallguys.common.response.ApiResponse;
import com.fallguys.common.security.CustomUserDetails;
import com.fallguys.mypage.api.web.dto.freelancer.response.FreelancerAiPositivityIndexDto;
import com.fallguys.common.ai.dto.FreelancerAiReputationReportDto;
import com.fallguys.mypage.api.web.dto.freelancer.response.FreelancerEvaluationSummaryDto;
import com.fallguys.mypage.api.web.dto.freelancer.response.FreelancerStrengthWeaknessDto;
import com.fallguys.mypage.service.freelancer.FreelancerReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "9. Freelancer MyPage - Grade & Review", description = "프리랜서 마이페이지 평점 및 리뷰 API")
@RestController
@RequestMapping("/api/freelancer/mypage/reviews")
@RequiredArgsConstructor
public class FreelancerReviewController {

    private final FreelancerReviewService freelancerReviewService;

    @Operation(summary = "내 평판/등급 요약 조회",
            description = "Redis에서 항목별 평점 데이터를 받아 전문성/의사소통/일정준수 항목별 평균 및 전체 평균을 조회합니다.")
    @GetMapping("/summary")
    public ApiResponse<FreelancerEvaluationSummaryDto> getReviewSummary(@AuthenticationPrincipal CustomUserDetails userDetails) {
        return ApiResponse.ok(freelancerReviewService.getReviewSummary(userDetails.getId()));
    }

    @Operation(summary = "AI 평판 분석 리포트 조회",
            description = "AI 도메인에서 분석한 프리랜서 평판 리포트를 조회합니다. (구현 예정)")
    @GetMapping("/ai/report")
    public ApiResponse<FreelancerAiReputationReportDto> getAiReputationReport(@AuthenticationPrincipal CustomUserDetails userDetails) {
        return ApiResponse.ok(freelancerReviewService.getAiReputationReport(userDetails.getId()));
    }

    @Operation(summary = "AI 평판 긍정 지수 조회",
            description = "AI 도메인에서 항목별 평판을 분석하여 전달한 긍정 지수를 조회합니다. (구현 예정)")
    @GetMapping("/ai/positivity")
    public ApiResponse<FreelancerAiPositivityIndexDto> getAiPositivityIndex(@AuthenticationPrincipal CustomUserDetails userDetails) {
        return ApiResponse.ok(freelancerReviewService.getAiPositivityIndex(userDetails.getId()));
    }

    @Operation(summary = "강점/약점 분석 조회",
            description = "AI가 분석한 프리랜서 강점 3가지, 약점 3가지를 조회합니다. (구현 예정)")
    @GetMapping("/ai/strength-weakness")
    public ApiResponse<FreelancerStrengthWeaknessDto> getStrengthWeaknessAnalysis(@AuthenticationPrincipal CustomUserDetails userDetails) {
        return ApiResponse.ok(freelancerReviewService.getStrengthWeaknessAnalysis(userDetails.getId()));
    }
}
