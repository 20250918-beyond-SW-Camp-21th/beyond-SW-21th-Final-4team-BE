package com.fallguys.mypage.api.web.employer;


import com.fallguys.common.response.ApiResponse;
import com.fallguys.common.security.CustomUserDetails;
import com.fallguys.mypage.api.web.dto.employer.response.EmployerReputationAiResponseDto;
import com.fallguys.mypage.api.web.dto.employer.response.EmployerReviewSummaryResponseDto;
import com.fallguys.mypage.service.employer.EmployerReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "3. Employer MyPage - Review", description = "고용주 마이페이지 리뷰 및 평판 API")
@RestController
@RequestMapping("/api/employer/mypage")
@RequiredArgsConstructor
public class EmployerReviewController {

    private final EmployerReviewService employerReviewService;

    @Operation(summary = "평판 요약 조회", description = "고용주가 받은 평가들의 항목별 요약 평균을 조회합니다.")
    @GetMapping("/reviews/summary")
    public ApiResponse<EmployerReviewSummaryResponseDto> getReviewSummary(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        EmployerReviewSummaryResponseDto response = employerReviewService.getReputationSummary(userDetails.getId());
        return ApiResponse.ok(response);
    }

    @Operation(summary = "AI 신뢰도 점수 및 리포트 조회", description = "PRIME 요금제에서 제공되는 AI 평판 리포트를 조회합니다.")
    @GetMapping("/reputation/ai")
    public ApiResponse<EmployerReputationAiResponseDto> getAiReputation(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        
        EmployerReputationAiResponseDto response = employerReviewService.getAiReputation(userDetails.getId());
        return ApiResponse.ok(response);
    }
}
