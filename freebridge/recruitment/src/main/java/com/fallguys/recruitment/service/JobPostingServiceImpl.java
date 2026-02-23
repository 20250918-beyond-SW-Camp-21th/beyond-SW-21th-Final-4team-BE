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

    /*공고 생성*/
    @Override
    public void createJobPosting(JobPostingCreateDTO jobPostingCreateDTO){
        JobPosting jobPosting=new JobPosting();
        jobPosting.create(jobPostingCreateDTO);
        jobPostingRepo.save(jobPosting);
    }

    /*공고 수정*/
    @Override
    public void updateJobPosting(JobPostingUpdateDTO jobPostingUpdateDTO,Long JobPostingId){
        JobPosting jobPosting=jobPostingRepo.getById(JobPostingId);
        jobPosting.update(jobPostingUpdateDTO);
    }

    /*공고 삭제*/
    @Override
    public void deleteJobPosting(Long JobPostingId){
        jobPostingRepo.deleteById(JobPostingId);
    }



}
