package com.fallguys.mypage.api.web.freelancer;

import com.fallguys.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "Freelancer MyPage - Grade", description = "프리랜서등급 관리 API")
@RestController
@RequestMapping("/api/mypage/freelancer/grade")
@RequiredArgsConstructor
public class FreelancerGradeController {

    @Operation(summary = "등급 산정 기준 조회", description = "프리랜서 등급 산정 기준표를 조회합니다.")
    @GetMapping("/criteria")
    public ApiResponse<List<Map<String, String>>> getGradeCriteria() {
        // TODO: 등급 산정 기준 반환 (GradeCriteriaItem)
        return ApiResponse.ok(null);
    }

    @Operation(summary = "학력 옵션 조회", description = "등급 산정을 위한 학력 선택 옵션을 조회합니다.")
    @GetMapping("/options/education")
    public ApiResponse<List<Map<String, String>>> getEducationOptions() {
        // TODO: 학력 옵션 반환 (EducationOption)
        return ApiResponse.ok(null);
    }

    @Operation(summary = "자격증 옵션 조회", description = "등급 산정을 위한 자격증 선택 옵션을 조회합니다.")
    @GetMapping("/options/certification")
    public ApiResponse<List<Map<String, String>>> getCertificationOptions() {
        // TODO: 자격증 옵션 반환 (CertificationOption)
        return ApiResponse.ok(null);
    }

    @Operation(summary = "등급 모의 계산", description = "입력된 정보를 바탕으로 예상 등급을 계산합니다.")
    @PostMapping("/calculate")
    public ApiResponse<String> calculateGrade(@RequestBody Map<String, Object> request) {
        // TODO: 등급 계산 로직 구현
        // Request: { type: 'education'|'certification', education?, certification?, yearsOfExperience }
        // Return: '특급' | '고급' | '중급' | '초급'
        return ApiResponse.ok(null);
    }

    @Operation(summary = "등급 정보 저장", description = "계산된 등급 정보를 저장합니다.")
    @PostMapping("/save")
    public ApiResponse<Void> saveGrade(@RequestHeader("X-User-Id") String userId,
                                       @RequestBody Map<String, Object> request) {
        // TODO: 등급 정보 저장 로직 구현
        return ApiResponse.ok(null);
    }
}
