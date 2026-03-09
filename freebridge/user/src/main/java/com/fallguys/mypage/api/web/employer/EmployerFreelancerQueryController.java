package com.fallguys.mypage.api.web.employer;

import com.fallguys.common.response.ApiResponse;
import com.fallguys.mypage.api.shared.SharedMypageApi;
import com.fallguys.mypage.api.web.dto.employer.response.EmployerFreelancerSearchResponseDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Employer MyPage - Freelancer Search", description = "고용주 마이페이지 프리랜서 검색/목록 API")
@RestController
@RequiredArgsConstructor
public class EmployerFreelancerQueryController {

    private final SharedMypageApi sharedMypageApi;

    @Operation(summary = "프리랜서 목록 조회 (페이징/검색)")
    @GetMapping("/api/employer/freelancers")
    public ApiResponse<EmployerFreelancerSearchResponseDto> getFreelancers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String keyword
    ) {
        return ApiResponse.ok(sharedMypageApi.getEmployerFreelancers(page, size, keyword));
    }
}
