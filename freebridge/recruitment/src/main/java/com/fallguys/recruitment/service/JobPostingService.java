package com.fallguys.recruitment.service;

import com.fallguys.recruitment.api.dto.request.JobPostingCreateDTO;
import com.fallguys.recruitment.api.dto.request.JobPostingUpdateDTO;
import com.fallguys.recruitment.api.dto.response.FreelancerJobPostingSearchDTO;
import com.fallguys.recruitment.api.dto.response.JobPostingSearchDTO;

import java.util.List;

public interface JobPostingService {
    void createJobPosting(JobPostingCreateDTO jobPostingCreateDTO, String userEmail);

    void updateJobPosting(JobPostingUpdateDTO jobPostingUpdateDTO, Long jobPostingId, String userEmail);

    void deleteJobPosting(Long jobPostingId, String userEmail);

    List<JobPostingSearchDTO> getJobPostings(String userEmail);

    List<JobPostingSearchDTO> getAllJobPostings();

    List<FreelancerJobPostingSearchDTO> searchJobPostingsForFreelancer(
            String userEmail,
            String keyword,
            boolean favoritesOnly
    );

    void addFavoriteJobPosting(String userEmail, Long jobPostingId);

    void removeFavoriteJobPosting(String userEmail, Long jobPostingId);

}
