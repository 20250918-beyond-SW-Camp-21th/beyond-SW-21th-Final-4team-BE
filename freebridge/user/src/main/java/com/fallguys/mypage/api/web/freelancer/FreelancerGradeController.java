package com.fallguys.mypage.api.web.freelancer;

import com.fallguys.common.response.ApiResponse;
import com.fallguys.common.security.CustomUserDetails;
import com.fallguys.mypage.api.web.dto.freelancer.request.GradeSaveRequestDto;
import com.fallguys.mypage.service.freelancer.FreelancerGradeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "11. Freelancer MyPage - Grade Save", description = "프리랜서 등급 저장 API")
@RestController
@RequestMapping("/api/freelancer/mypage/grade")
@RequiredArgsConstructor
public class FreelancerGradeController {

    private final FreelancerGradeService freelancerGradeService;

    @Operation(summary = "프리랜서 등급 저장", description = "계산된 등급 정보를 저장합니다.")
    @PostMapping
    public ApiResponse<Void> saveGrade(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody GradeSaveRequestDto request) {
        freelancerGradeService.saveGrade(userDetails.getId(), request);
        return ApiResponse.ok(null);
    }
}
