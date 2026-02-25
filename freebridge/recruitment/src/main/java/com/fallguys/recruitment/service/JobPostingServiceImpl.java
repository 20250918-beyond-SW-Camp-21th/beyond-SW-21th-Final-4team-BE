package com.fallguys.recruitment.service;

import com.fallguys.common.exception.BusinessException;
import com.fallguys.common.exception.ErrorCode;
import com.fallguys.recruitment.api.dto.request.JobPostingCreateDTO;
import com.fallguys.recruitment.api.dto.request.JobPostingUpdateDTO;
import com.fallguys.recruitment.api.dto.response.JobPostingSearchDTO;
import com.fallguys.recruitment.entity.JobPosting;
import com.fallguys.recruitment.entity.Status;
import com.fallguys.recruitment.repository.JobPostingRepo;
import com.fallguys.user.entity.Role;
import com.fallguys.user.entity.User;
import com.fallguys.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class JobPostingServiceImpl implements JobPostingService {

    private final JobPostingRepo jobPostingRepo;
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public List<JobPostingSearchDTO> getJobPostings(Long userId) {

        User user = getEmployerOrThrow(userId);

        return jobPostingRepo
                .findAllByEmployerIdAndDeletedFalse(user.getId());
    }

    @Override
    @Transactional
    public void createJobPosting(JobPostingCreateDTO jobPostingCreateDTO, Long userId) {
        User user = getEmployerOrThrow(userId);
        JobPosting jobPosting = JobPosting.from(jobPostingCreateDTO, user.getId(), user.getName());
        jobPostingRepo.save(jobPosting);
    }

    @Override
    @Transactional
    public void updateJobPosting(JobPostingUpdateDTO jobPostingUpdateDTO, Long jobPostingId, Long userId) {
        getEmployerOrThrow(userId);
        JobPosting jobPosting = getJobPostingOrThrow(jobPostingId);
        validateOwnership(jobPosting, userId);
        validateNotDeleted(jobPosting);
        jobPosting.update(jobPostingUpdateDTO);
    }

    @Override
    @Transactional
    public void deleteJobPosting(Long jobPostingId, Long userId) {
        getEmployerOrThrow(userId);
        JobPosting jobPosting = getJobPostingOrThrow(jobPostingId);
        validateOwnership(jobPosting, userId);
        validateNotDeleted(jobPosting);
        jobPosting.delete();
    }

    private User getEmployerOrThrow(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        if (user.getRole() != Role.EMPLOYER) {
            throw new BusinessException(ErrorCode.ONLY_EMPLOYER_ALLOWED);
        }
        return user;
    }

    private JobPosting getJobPostingOrThrow(Long jobPostingId) {
        return jobPostingRepo.findById(jobPostingId)
                .orElseThrow(() -> new BusinessException(ErrorCode.JOB_POSTING_NOT_FOUND));
    }

    private void validateOwnership(JobPosting jobPosting, Long userId) {
        if (!jobPosting.getEmployerId().equals(userId)) {
            throw new BusinessException(ErrorCode.JOB_POSTING_FORBIDDEN);
        }
    }

    private void validateNotDeleted(JobPosting jobPosting) {
        if (jobPosting.getStatus() == Status.DELETED) {
            throw new BusinessException(ErrorCode.JOB_POSTING_ALREADY_DELETED);
        }
    }
}
