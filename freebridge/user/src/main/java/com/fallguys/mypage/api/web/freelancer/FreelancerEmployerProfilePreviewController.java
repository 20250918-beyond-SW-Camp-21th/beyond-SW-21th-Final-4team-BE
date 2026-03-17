package com.fallguys.mypage.api.web.freelancer;

import com.fallguys.common.response.ApiResponse;
import com.fallguys.mypage.api.web.dto.employer.response.EmployerProfilePreviewResponseDto;
import com.fallguys.mypage.service.ProfilePreviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/freelancer/employers")
public class FreelancerEmployerProfilePreviewController {

    private final ProfilePreviewService profilePreviewService;

    @GetMapping("/{employerId}/preview")
    public ApiResponse<EmployerProfilePreviewResponseDto> getEmployerPreview(@PathVariable Long employerId) {
        return ApiResponse.ok(profilePreviewService.getEmployerPreview(employerId));
    }
}
