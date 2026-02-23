package com.fallguys.recruitment.service;

import com.fallguys.recruitment.api.dto.request.JobPostingCreateDTO;
import com.fallguys.recruitment.api.dto.request.JobPostingUpdateDTO;
import com.fallguys.recruitment.entity.JobPosting;
import com.fallguys.recruitment.repository.JobPostingRepo;
import com.fallguys.user.entity.User;
import com.fallguys.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class JobPostingServiceImpl implements JobPostingService {

    final JobPostingRepo jobPostingRepo;
    final UserRepository userRepository;

    /*공고 생성*/
    @Override
    public void createJobPosting(JobPostingCreateDTO jobPostingCreateDTO,Long userId) {
        User user=userRepository.findById(userId).orElseThrow();
        JobPosting jobPosting=new JobPosting();
        jobPosting.create(jobPostingCreateDTO,user.getId(),user.getName());
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
