package com.fallguys.mypage.api.web.freelancer;

import com.fallguys.common.response.ApiResponse;
import com.fallguys.mypage.api.web.dto.freelancer.response.FreelancerEvaluationSummaryDto;
import com.fallguys.mypage.api.web.dto.freelancer.response.FreelancerReviewListDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "9. Freelancer MyPage - Grade & Review", description = "프리랜서 마이페이지 평점 및 리뷰 API")
@RestController
@RequestMapping("/api/freelancer/mypage/reviews")
@RequiredArgsConstructor
public class FreelancerReviewController {

    @Operation(summary = "내 평판/등급 요약 조회", description = "평균 평점 및 상위 퍼센타일 정보를 조회합니다.")
    @GetMapping("/summary")
    public ApiResponse<FreelancerEvaluationSummaryDto> getReviewSummary(@RequestHeader("X-User-Id") String userId) {
        return ApiResponse.ok(null);
    }

    @Operation(summary = "고용주가 남긴 리뷰 목록 조회", description = "자신이 함께 작업했던 고용주들이 남긴 리뷰 내역을 조회합니다.")
    @GetMapping
    public ApiResponse<List<FreelancerReviewListDto>> getReviewList(@RequestHeader("X-User-Id") String userId) {
        return ApiResponse.ok(null);
    }
}
