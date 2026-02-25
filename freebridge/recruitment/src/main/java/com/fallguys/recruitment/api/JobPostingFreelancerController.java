package com.fallguys.recruitment.api;

import com.fallguys.common.response.ApiResponse;
import com.fallguys.recruitment.api.dto.response.FreelancerJobPostingSearchDTO;
import com.fallguys.recruitment.service.JobPostingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/freelancers/job-postings")
@RequiredArgsConstructor
public class JobPostingFreelancerController {

    private final JobPostingService jobPostingService;

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<FreelancerJobPostingSearchDTO>>> searchJobPostings(
            @RequestParam Long freelancerId,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "false") boolean favoritesOnly
    ) {
        List<FreelancerJobPostingSearchDTO> result =
                jobPostingService.searchJobPostingsForFreelancer(freelancerId, keyword, favoritesOnly);

        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @PostMapping("/{jobPostingId}/favorites")
    public ResponseEntity<ApiResponse<Void>> addFavorite(
            @RequestParam Long freelancerId,
            @PathVariable Long jobPostingId
    ) {
        jobPostingService.addFavoriteJobPosting(freelancerId, jobPostingId);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @DeleteMapping("/{jobPostingId}/favorites")
    public ResponseEntity<ApiResponse<Void>> removeFavorite(
            @RequestParam Long freelancerId,
            @PathVariable Long jobPostingId
    ) {
        jobPostingService.removeFavoriteJobPosting(freelancerId, jobPostingId);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}
