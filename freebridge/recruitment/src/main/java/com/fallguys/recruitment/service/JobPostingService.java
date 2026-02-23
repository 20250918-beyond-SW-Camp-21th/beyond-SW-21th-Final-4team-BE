package com.fallguys.recruitment.service;

import com.fallguys.recruitment.api.dto.request.JobPostingCreateDTO;
import com.fallguys.recruitment.api.dto.request.JobPostingUpdateDTO;

public interface JobPostingService {
    void createJobPosting(JobPostingCreateDTO jobPostingCreateDTO, Long userId);

    void updateJobPosting(JobPostingUpdateDTO jobPostingUpdateDTO, Long jobPostingId);

    void deleteJobPosting(Long jobPostingId);

}
