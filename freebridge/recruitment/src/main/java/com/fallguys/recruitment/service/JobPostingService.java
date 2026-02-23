package com.fallguys.recruitment.service;

import com.fallguys.recruitment.api.dto.request.JobPostingCreateDTO;
public interface JobPostingService {
    public void createJobPosting(JobPostingCreateDTO jobPostingCreateDTO);
}
