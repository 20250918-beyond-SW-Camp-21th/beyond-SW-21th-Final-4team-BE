package com.fallguys.recruitment.api;

import com.fallguys.common.response.ApiResponse;
import com.fallguys.recruitment.api.dto.response.FreelancerJobPostingSearchDTO;
import com.fallguys.recruitment.api.dto.response.PagedResponseDTO;
import com.fallguys.recruitment.api.util.PagingUtils;
import com.fallguys.recruitment.service.JobPostingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class JobPostingFreelancerController {

    private final JobPostingService jobPostingService;

    @GetMapping("/api/v1/freelancer/jobs")
    public ResponseEntity<ApiResponse<PagedResponseDTO<FreelancerJobPostingSearchDTO>>> searchJobPostings(
            @RequestParam Long freelancerId,
            @RequestParam(required = false) String keyword,
            @RequestParam(name = "liked", defaultValue = "false") boolean liked,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        List<FreelancerJobPostingSearchDTO> result =
                jobPostingService.searchJobPostingsForFreelancer(freelancerId, keyword, liked);
        PagedResponseDTO<FreelancerJobPostingSearchDTO> paged = PagingUtils.toPagedResponse(result, page, size);

        return ResponseEntity.ok(ApiResponse.ok(paged));
    }

    @PostMapping("/api/v1/freelancer/jobs/{jobPostingId}/like")
    public ResponseEntity<ApiResponse<Void>> addFavorite(
            @RequestParam Long freelancerId,
            @PathVariable Long jobPostingId
    ) {
        jobPostingService.addFavoriteJobPosting(freelancerId, jobPostingId);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @DeleteMapping("/api/v1/freelancer/jobs/{jobPostingId}/like")
    public ResponseEntity<ApiResponse<Void>> removeFavorite(
            @RequestParam Long freelancerId,
            @PathVariable Long jobPostingId
    ) {
        jobPostingService.removeFavoriteJobPosting(freelancerId, jobPostingId);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}
