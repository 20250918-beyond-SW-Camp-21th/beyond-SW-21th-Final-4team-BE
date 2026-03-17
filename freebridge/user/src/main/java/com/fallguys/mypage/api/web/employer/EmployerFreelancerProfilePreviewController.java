package com.fallguys.mypage.api.web.employer;

import com.fallguys.common.response.ApiResponse;
import com.fallguys.mypage.api.web.dto.freelancer.response.FreelancerProfilePreviewResponseDto;
import com.fallguys.mypage.service.ProfilePreviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/employer/freelancers")
public class EmployerFreelancerProfilePreviewController {

    private final ProfilePreviewService profilePreviewService;

    @GetMapping("/{freelancerId}/preview")
    public ApiResponse<FreelancerProfilePreviewResponseDto> getFreelancerPreview(@PathVariable Long freelancerId) {
        return ApiResponse.ok(profilePreviewService.getFreelancerPreview(freelancerId));
    }
}
