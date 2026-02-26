package com.fallguys.mypage.api.web.freelancer;

import com.fallguys.common.response.ApiResponse;
import com.fallguys.mypage.dto.freelancer.response.FreelancerEvaluationSummaryDto;
import com.fallguys.mypage.dto.freelancer.response.FreelancerReviewListDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

import java.util.List;

@Tag(name = "Freelancer MyPage - Grade & Review", description = "프리랜서 마이페이지 평점 및 리뷰 API")
@RestController
@RequestMapping("/api/freelancer/mypage/evaluations")
@RequiredArgsConstructor
public class FreelancerReviewController {

    @Operation(summary = "내 평판/등급 요약 조회", description = "평균 평점 및 상위 퍼센타일 정보를 조회합니다.")
    @GetMapping("/summary")
    public ApiResponse<FreelancerEvaluationSummaryDto> getReviewSummary(@AuthenticationPrincipal String userId) {
        return ApiResponse.ok(null);
    }

    @Operation(summary = "고용주가 남긴 리뷰 목록 조회", description = "자신이 함께 작업했던 고용주들이 남긴 리뷰 내역을 조회합니다.")
    @GetMapping
    public ApiResponse<List<FreelancerReviewListDto>> getReviewList(@AuthenticationPrincipal String userId) {
        return ApiResponse.ok(null);
    }

    @Operation(summary = "프리랜서 AI 평가 분석 조회", description = "고용주 평가 기반으로 AI가 분석한 한줄평, 평판지수, 강/약점을 반환합니다.")
    @GetMapping("/ai")
    public ApiResponse<Object> getFreelancerAiEvaluation(@AuthenticationPrincipal String userId) {
        return ApiResponse.ok(null);
    }
}
