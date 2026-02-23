package com.fallguys.recruitment.service;

import com.fallguys.recruitment.api.dto.request.JobPostingCreateDTO;
import com.fallguys.recruitment.api.dto.request.JobPostingUpdateDTO;
import com.fallguys.recruitment.entity.JobPosting;
import com.fallguys.recruitment.entity.JobPostingStatus;
import com.fallguys.recruitment.repository.JobPostingRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

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
                .status(JobPostingStatus.OPEN)
                .description(jobPostingCreateDTO.getDescription())
                .techStack(jobPostingCreateDTO.getTechStack())
                .duration(jobPostingCreateDTO.getDuration())
                .createdAt(LocalDateTime.now()).build();

        jobPostingRepo.save(jobPosting);
    }

    @Override
    public void updateJobPosting(JobPostingUpdateDTO jobPostingUpdateDTO,Long JobPostingId){
        JobPosting jobPosting=jobPostingRepo.getById(JobPostingId);
        jobPosting.update(jobPostingUpdateDTO);
    }
}
