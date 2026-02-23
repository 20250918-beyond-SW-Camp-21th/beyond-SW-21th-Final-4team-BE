package com.fallguys.recruitment.service;

import com.fallguys.recruitment.api.dto.request.JobPostingCreateDTO;
import com.fallguys.recruitment.api.dto.request.JobPostingUpdateDTO;

public interface JobPostingService {
    public void createJobPosting(JobPostingCreateDTO jobPostingCreateDTO);
    //TODO: user 입력되면 유저로 employerID 등록하기

    public void updateJobPosting(JobPostingUpdateDTO jobPostingUpdateDTO,Long jobPostingId);

    public void deleteJobPosting(Long jobPostingId);

}
