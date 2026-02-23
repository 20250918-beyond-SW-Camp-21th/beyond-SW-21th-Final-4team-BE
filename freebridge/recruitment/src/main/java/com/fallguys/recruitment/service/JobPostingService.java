package com.fallguys.recruitment.service;

import com.fallguys.recruitment.entity.JobPosting;
import com.fallguys.recruitment.repository.JobPostingRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class JobPostingService implements JobPostingServiceImpl{

    @Autowired
    private JobPostingRepo jobPostingRepo;


    @Override
    public void createJobPosting(JobPosting jobPosting) {

    }
}
