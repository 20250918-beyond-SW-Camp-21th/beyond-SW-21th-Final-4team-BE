package com.fallguys.recruitment.service;

import com.fallguys.recruitment.api.dto.request.JobPostingCreateDTO;
import com.fallguys.recruitment.api.dto.request.JobPostingUpdateDTO;
import com.fallguys.recruitment.entity.JobPosting;
import com.fallguys.recruitment.repository.JobPostingRepo;
import com.fallguys.user.entity.User;
import com.fallguys.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class JobPostingServiceImpl implements JobPostingService {

    private final JobPostingRepo jobPostingRepo;
    private final UserRepository userRepository;

    /*생성*/
    @Override
    @Transactional
    public void createJobPosting(JobPostingCreateDTO jobPostingCreateDTO, Long userId) {
        User user = userRepository.findById(userId).orElseThrow();
        JobPosting jobPosting = JobPosting.from(jobPostingCreateDTO, user.getId(), user.getName());
        jobPostingRepo.save(jobPosting);
    }

    /*수정*/
    @Override
    @Transactional
    public void updateJobPosting(JobPostingUpdateDTO jobPostingUpdateDTO, Long jobPostingId, Long userId) {
        JobPosting jobPosting = jobPostingRepo.findById(jobPostingId).orElseThrow();
        jobPosting.update(jobPostingUpdateDTO);
    }

    /*삭제*/
    @Override
    @Transactional
    public void deleteJobPosting(Long jobPostingId, Long userId) {
        JobPosting jobPosting = jobPostingRepo.findById(jobPostingId).orElseThrow();
        jobPosting.delete();
    }
}
