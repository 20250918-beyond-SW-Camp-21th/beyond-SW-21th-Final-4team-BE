package com.fallguys.recruitment.service;

import com.fallguys.recruitment.api.dto.request.JobPostingCreateDTO;
import com.fallguys.recruitment.api.dto.request.JobPostingUpdateDTO;
import com.fallguys.recruitment.api.dto.response.AiRecommendationResponseDTO;
import com.fallguys.recruitment.api.dto.response.EmployerProjectSearchDTO;
import com.fallguys.recruitment.api.dto.response.FreelancerJobPostingSearchDTO;
import com.fallguys.recruitment.api.dto.response.JobPostingSearchDTO;
import com.fallguys.recruitment.api.dto.response.MatchedFreelancerResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface JobPostingService {
    void createJobPosting(JobPostingCreateDTO jobPostingCreateDTO, Long userId);

    void updateJobPosting(JobPostingUpdateDTO jobPostingUpdateDTO, Long jobPostingId, Long userId);

    void deleteJobPosting(Long jobPostingId, Long userId);

    List<JobPostingSearchDTO> getJobPostings(Long userId);

    List<JobPostingSearchDTO> getAllJobPostings();

    List<EmployerProjectSearchDTO> getEmployerProjects(Long userId);

    Page<MatchedFreelancerResponseDTO> getMatchedFreelancers(Long projectId, Long userId, Pageable pageable);

    List<FreelancerJobPostingSearchDTO> searchJobPostingsForFreelancer(
            Long userId,
            String keyword,
            boolean favoritesOnly
    );

    void addFavoriteJobPosting(Long userId, Long jobPostingId);

    void removeFavoriteJobPosting(Long userId, Long jobPostingId);

    List<AiRecommendationResponseDTO> getRecommendedFreelancers(Long jobPostingId, Long userId);

    List<AiRecommendationResponseDTO> getRecommendedJobsForFreelancer(Long userId);

    void completeProject(Long projectId, Long userId);

    void closeJobPosting(Long jobPostingId);

    void refreshEmployerRecruitmentCaches(Long employerId);

    void refreshEmployerProjectStatsCache(Long employerId);

    void triggerFreelancerRecommendation(Long jobPostingId, Long userId);

    void triggerJobRecommendation(Long userId);
}
