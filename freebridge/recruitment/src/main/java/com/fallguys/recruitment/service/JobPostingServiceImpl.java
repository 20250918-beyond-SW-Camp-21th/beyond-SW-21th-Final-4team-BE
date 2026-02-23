package com.fallguys.recruitment.service;

import com.fallguys.recruitment.api.dto.request.JobPostingCreateDTO;
import com.fallguys.recruitment.entity.JobPosting;
import com.fallguys.recruitment.repository.JobPostingRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;

@Service
public class JobPostingServiceImpl implements JobPostingService {

    @Autowired
    JobPostingRepo jobPostingRepo;

    @Override
    public void createJobPosting(JobPostingCreateDTO jobPostingCreateDTO){
        JobPosting jobPosting = JobPosting.builder()
                .employerId(jobPostingCreateDTO.getEmployerId())
                .employerName(jobPostingCreateDTO.getEmployerName())
                .title(jobPostingCreateDTO.getTitle())
                .description(jobPostingCreateDTO.getDescription())
                .techStack(jobPostingCreateDTO.getTechStack())
                .duration(jobPostingCreateDTO.getDuration())
                .createdAt(jobPostingCreateDTO.getCreatedAt()).build();

        jobPostingRepo.save(jobPosting);
    }
}
