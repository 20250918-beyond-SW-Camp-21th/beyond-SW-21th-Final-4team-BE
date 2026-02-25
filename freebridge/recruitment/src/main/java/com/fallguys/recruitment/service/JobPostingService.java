package com.fallguys.recruitment.service;

import com.fallguys.recruitment.api.dto.request.JobPostingCreateDTO;
import com.fallguys.recruitment.api.dto.request.JobPostingUpdateDTO;
import com.fallguys.recruitment.api.dto.response.JobPostingSearchDTO;
import com.fallguys.recruitment.entity.JobPosting;

import java.util.List;

public interface JobPostingService {
    void createJobPosting(JobPostingCreateDTO jobPostingCreateDTO, Long userId);

    void updateJobPosting(JobPostingUpdateDTO jobPostingUpdateDTO, Long jobPostingId,Long userId);

    void deleteJobPosting(Long jobPostingId,Long userId);

    List<JobPostingSearchDTO> getJobPostings(Long userId);


}
