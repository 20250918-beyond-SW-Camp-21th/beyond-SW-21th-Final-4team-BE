package com.fallguys.mypage.api.web.freelancer;

import com.fallguys.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "Freelancer MyPage - Evaluation", description = "프리랜서 평가/피드백 조회 API")
@RestController
@RequestMapping("/api/mypage/freelancer/evaluations")
@RequiredArgsConstructor
public class FreelancerEvaluationController {

    @Operation(summary = "받은 평가 조회", description = "완료된 프로젝트에 대해 고용주로부터 받은 평가를 조회합니다.")
    @GetMapping
    public ApiResponse<List<Map<String, Object>>> getEvaluations(@RequestHeader("X-User-Id") String userId) {
        // TODO: 평가 목록 조회 로직 구현
        // Evaluation DTO 반환
        return ApiResponse.ok(null);
    }

    @Operation(summary = "불합격 피드백 조회", description = "지원한 프로젝트의 불합격 사유 및 피드백을 조회합니다.")
    @GetMapping("/rejections")
    public ApiResponse<List<Map<String, Object>>> getRejectionFeedbacks(@RequestHeader("X-User-Id") String userId) {
        // TODO: 불합격 피드백 목록 조회 로직 구현
        // RejectionFeedback DTO 반환
        return ApiResponse.ok(null);
    }
}
