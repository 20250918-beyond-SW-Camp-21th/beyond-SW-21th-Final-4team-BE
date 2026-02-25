package com.fallguys.recruitment.service;

import com.fallguys.common.exception.BusinessException;
import com.fallguys.common.exception.ErrorCode;
import com.fallguys.recruitment.api.dto.request.JobPostingCreateDTO;
import com.fallguys.recruitment.api.dto.request.JobPostingUpdateDTO;
import com.fallguys.recruitment.api.dto.response.FreelancerJobPostingSearchDTO;
import com.fallguys.recruitment.api.dto.response.JobPostingSearchDTO;
import com.fallguys.recruitment.entity.JobPostingFavorite;
import com.fallguys.recruitment.entity.JobPosting;
import com.fallguys.recruitment.entity.JobPostingStatus;
import com.fallguys.recruitment.entity.Status;
import com.fallguys.recruitment.repository.JobPostingFavoriteRepo;
import com.fallguys.recruitment.repository.JobPostingRepo;
import com.fallguys.user.entity.Role;
import com.fallguys.user.entity.User;
import com.fallguys.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class JobPostingServiceImpl implements JobPostingService {

    private final JobPostingRepo jobPostingRepo;
    private final JobPostingFavoriteRepo jobPostingFavoriteRepo;
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public List<JobPostingSearchDTO> getJobPostings(Long userId) {
        User user = getEmployerOrThrow(userId);
        return jobPostingRepo.findAllByEmployerIdAndStatusNot(user.getId(), Status.DELETED)
                .stream()
                .map(this::toJobPostingSearchDto)
                .toList();
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

    @Override
    public List<JobPostingSearchDTO> getAllJobPostings() {
        return jobPostingRepo.findAllByStatusNot(Status.DELETED)
                .stream()
                .map(this::toJobPostingSearchDto)
                .toList();
    }

    @Override
    public List<FreelancerJobPostingSearchDTO> searchJobPostingsForFreelancer(Long freelancerId, String keyword, boolean favoritesOnly) {
        getFreelancerOrThrow(freelancerId);

        Set<Long> favoriteJobPostingIds = new HashSet<>(
                jobPostingFavoriteRepo.findAllByFreelancerId(freelancerId)
                        .stream()
                        .map(JobPostingFavorite::getJobPostingId)
                        .toList()
        );

        String normalizedKeyword = keyword == null ? "" : keyword.trim().toLowerCase(Locale.ROOT);

        return jobPostingRepo.findAllByStatusAndPostingStatus(Status.ACTIVE, JobPostingStatus.OPEN)
                .stream()
                .filter(jobPosting -> matchesKeyword(jobPosting, normalizedKeyword))
                .filter(jobPosting -> !favoritesOnly || favoriteJobPostingIds.contains(jobPosting.getId()))
                .map(jobPosting -> toFreelancerSearchDto(jobPosting, favoriteJobPostingIds.contains(jobPosting.getId())))
                .toList();
    }

    @Override
    @Transactional
    public void addFavoriteJobPosting(Long freelancerId, Long jobPostingId) {
        getFreelancerOrThrow(freelancerId);

        JobPosting jobPosting = getJobPostingOrThrow(jobPostingId);
        validateNotDeleted(jobPosting);

        try {
            jobPostingFavoriteRepo.save(JobPostingFavorite.of(freelancerId, jobPostingId));
        } catch (DataIntegrityViolationException ignored) {
            // Duplicate favorite is treated as idempotent no-op.
        }
    }

    @Override
    @Transactional
    public void removeFavoriteJobPosting(Long freelancerId, Long jobPostingId) {
        getFreelancerOrThrow(freelancerId);

        jobPostingFavoriteRepo.deleteByFreelancerIdAndJobPostingId(freelancerId, jobPostingId);
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

    private User getFreelancerOrThrow(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        if (user.getRole() != Role.FREELANCER) {
            throw new BusinessException(ErrorCode.ONLY_FREELANCER_ALLOWED);
        }
        return user;
    }

    private JobPostingSearchDTO toJobPostingSearchDto(JobPosting jobPosting) {
        return new JobPostingSearchDTO(
                jobPosting.getId(),
                jobPosting.getEmployerName(),
                jobPosting.getTitle(),
                jobPosting.getDescription(),
                new ArrayList<>(jobPosting.getTechStack()),
                jobPosting.getBudget(),
                jobPosting.getDuration(),
                jobPosting.getPostingStatus()
        );
    }

    private FreelancerJobPostingSearchDTO toFreelancerSearchDto(JobPosting jobPosting, boolean favorite) {
        return new FreelancerJobPostingSearchDTO(
                jobPosting.getId(),
                jobPosting.getEmployerName(),
                jobPosting.getTitle(),
                jobPosting.getDescription(),
                new ArrayList<>(jobPosting.getTechStack()),
                jobPosting.getBudget(),
                jobPosting.getDuration(),
                favorite
        );
    }

    private boolean matchesKeyword(JobPosting jobPosting, String keyword) {
        if (keyword.isBlank()) {
            return true;
        }

        if (containsIgnoreCase(jobPosting.getTitle(), keyword) || containsIgnoreCase(jobPosting.getDescription(), keyword)) {
            return true;
        }

        return jobPosting.getTechStack().stream()
                .anyMatch(tech -> containsIgnoreCase(tech, keyword));
    }

    private boolean containsIgnoreCase(String source, String keyword) {
        return source != null && source.toLowerCase(Locale.ROOT).contains(keyword);
    }
}
