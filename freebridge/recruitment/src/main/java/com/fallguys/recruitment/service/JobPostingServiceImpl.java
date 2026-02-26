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
import com.fallguys.recruitment.service.port.RecruitmentUser;
import com.fallguys.recruitment.service.port.RecruitmentUserReader;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.EnumSet;
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
    private final RecruitmentUserReader recruitmentUserReader;

    @Override
    @Transactional(readOnly = true)
    public List<JobPostingSearchDTO> getJobPostings(String userEmail) {
        RecruitmentUser user = recruitmentUserReader.getEmployerByEmailOrThrow(userEmail);
        return jobPostingRepo.findAllByEmployerIdAndStatusNot(user.id(), Status.DELETED)
                .stream()
                .map(this::toJobPostingSearchDto)
                .toList();
    }

    @Override
    @Transactional
    public void createJobPosting(JobPostingCreateDTO jobPostingCreateDTO, String userEmail) {
        RecruitmentUser user = recruitmentUserReader.getEmployerByEmailOrThrow(userEmail);
        JobPosting jobPosting = JobPosting.from(jobPostingCreateDTO, user.id(), user.name());
        jobPostingRepo.save(jobPosting);
    }

    @Override
    @Transactional
    public void updateJobPosting(JobPostingUpdateDTO jobPostingUpdateDTO, Long jobPostingId, String userEmail) {
        RecruitmentUser user = recruitmentUserReader.getEmployerByEmailOrThrow(userEmail);
        JobPosting jobPosting = getJobPostingOrThrow(jobPostingId);
        validateOwnership(jobPosting, user.id());
        validateNotDeleted(jobPosting);
        try {
            jobPosting.update(jobPostingUpdateDTO);
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
    }

    @Override
    @Transactional
    public void deleteJobPosting(Long jobPostingId, String userEmail) {
        RecruitmentUser user = recruitmentUserReader.getEmployerByEmailOrThrow(userEmail);
        JobPosting jobPosting = getJobPostingOrThrow(jobPostingId);
        validateOwnership(jobPosting, user.id());
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
    public List<FreelancerJobPostingSearchDTO> searchJobPostingsForFreelancer(String userEmail, String keyword, boolean favoritesOnly) {
        RecruitmentUser user = recruitmentUserReader.getFreelancerByEmailOrThrow(userEmail);
        Long freelancerId = user.id();

        Set<Long> favoriteJobPostingIds = new HashSet<>(
                jobPostingFavoriteRepo.findAllByFreelancerId(freelancerId)
                        .stream()
                        .map(JobPostingFavorite::getJobPostingId)
                        .toList()
        );

        String normalizedKeyword = keyword == null ? "" : keyword.trim().toLowerCase(Locale.ROOT);

        return jobPostingRepo.findAllByStatusAndPostingStatusIn(
                        Status.ACTIVE,
                        EnumSet.of(JobPostingStatus.OPEN, JobPostingStatus.IN_PROGRESS)
                )
                .stream()
                .filter(jobPosting -> matchesKeyword(jobPosting, normalizedKeyword))
                .filter(jobPosting -> !favoritesOnly || favoriteJobPostingIds.contains(jobPosting.getId()))
                .map(jobPosting -> toFreelancerSearchDto(jobPosting, favoriteJobPostingIds.contains(jobPosting.getId())))
                .toList();
    }

    @Override
    @Transactional
    public void addFavoriteJobPosting(String userEmail, Long jobPostingId) {
        RecruitmentUser user = recruitmentUserReader.getFreelancerByEmailOrThrow(userEmail);
        Long freelancerId = user.id();

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
    public void removeFavoriteJobPosting(String userEmail, Long jobPostingId) {
        RecruitmentUser user = recruitmentUserReader.getFreelancerByEmailOrThrow(userEmail);
        Long freelancerId = user.id();

        jobPostingFavoriteRepo.deleteByFreelancerIdAndJobPostingId(freelancerId, jobPostingId);
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

    private JobPostingSearchDTO toJobPostingSearchDto(JobPosting jobPosting) {
        return new JobPostingSearchDTO(
                jobPosting.getId(),
                jobPosting.getEmployerName(),
                jobPosting.getTitle(),
                jobPosting.getDescription(),
                new ArrayList<>(jobPosting.getTechStack()),
                jobPosting.getBudget(),
                jobPosting.getDuration(),
                jobPosting.getHeadcount(),
                jobPosting.getMatchedHeadcount(),
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
                jobPosting.getHeadcount(),
                jobPosting.getMatchedHeadcount(),
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
