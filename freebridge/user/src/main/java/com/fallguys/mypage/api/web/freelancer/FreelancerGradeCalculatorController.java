package com.fallguys.mypage.api.web.freelancer;

import com.fallguys.common.response.ApiResponse;
import com.fallguys.common.security.CustomUserDetails;
import com.fallguys.mypage.api.web.dto.freelancer.request.GradeCalculationRequestDto;
import com.fallguys.mypage.api.web.dto.freelancer.response.GradeCalculationResultDto;
import com.fallguys.mypage.service.freelancer.FreelancerGradeCalculatorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "10. Freelancer MyPage - Grade Calculator", description = "프리랜서 등급(초급/중급/고급/특급) 산정 API")
@RestController
@RequestMapping("/api/freelancer/mypage/grade-calculator")
@RequiredArgsConstructor
public class FreelancerGradeCalculatorController {

    private final FreelancerGradeCalculatorService freelancerGradeCalculatorService;

    @Operation(
            summary = "등급 산정 및 저장",
            description = """
                    학경력자 또는 자격자 방식으로 등급을 계산하고 내 프로필에 반영합니다.

                    - **학경력자**: qualificationType=ACADEMIC_CAREER, degree(ASSOCIATE/BACHELOR/MASTER/DOCTOR), careerYears
                    - **자격자**  : qualificationType=LICENSED, licenseGrade(TECHNICIAN/INDUSTRIAL_ENGINEER/ENGINEER/ENGINEER_PROFESSIONAL), careerYears
                    """
    )
    @PostMapping("/calculate")
    public ApiResponse<GradeCalculationResultDto> calculate(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody GradeCalculationRequestDto request) {
        return ApiResponse.ok(freelancerGradeCalculatorService.calculate(request));
    }
}
