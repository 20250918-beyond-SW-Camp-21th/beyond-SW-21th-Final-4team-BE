package com.fallguys.recruitment.service;

import com.fallguys.recruitment.api.dto.request.JobPostingCreateDTO;
import com.fallguys.recruitment.api.dto.request.JobPostingUpdateDTO;
import com.fallguys.recruitment.api.dto.response.JobPostingSearchDTO;

import java.util.List;

public interface JobPostingService {
    void createJobPosting(JobPostingCreateDTO jobPostingCreateDTO, Long userId);

    void updateJobPosting(JobPostingUpdateDTO jobPostingUpdateDTO, Long jobPostingId,Long userId);

    void deleteJobPosting(Long jobPostingId,Long userId);

    //TODO:paging
    List<JobPostingSearchDTO> getJobPostings(Long userId);



}
