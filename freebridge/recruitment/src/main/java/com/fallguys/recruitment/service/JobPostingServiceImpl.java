package com.fallguys.recruitment.service;

import com.fallguys.common.ai.port.RecommendationEngine;
import com.fallguys.common.exception.BusinessException;
import com.fallguys.common.exception.ErrorCode;
import com.fallguys.recruitment.api.dto.request.JobPostingCreateDTO;
import com.fallguys.recruitment.api.dto.request.JobPostingUpdateDTO;
import com.fallguys.recruitment.api.dto.response.AiRecommendationResponseDTO;
import com.fallguys.recruitment.api.dto.response.EmployerProjectSearchDTO;
import com.fallguys.recruitment.api.dto.response.FreelancerJobPostingSearchDTO;
import com.fallguys.recruitment.api.dto.response.JobPostingSearchDTO;
import com.fallguys.recruitment.entity.JobPostingFavorite;
import com.fallguys.recruitment.entity.JobPosting;
import com.fallguys.recruitment.entity.JobPostingStatus;
import com.fallguys.recruitment.entity.Project;
import com.fallguys.recruitment.entity.Status;
import com.fallguys.recruitment.repository.JobPostingFavoriteRepo;
import com.fallguys.recruitment.repository.JobPostingRepo;
import com.fallguys.recruitment.repository.ProjectPostingRepo;
import com.fallguys.recruitment.service.port.RecruitmentUser;
import com.fallguys.recruitment.service.port.RecruitmentUserReader;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionSynchronization;
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
    private final ProjectPostingRepo projectPostingRepo;
    private final RecruitmentUserReader recruitmentUserReader;
    private final RecommendationEngine recommendationEngine; // AiAdapter 주입

    @Override
    @Transactional(readOnly = true)
    public List<JobPostingSearchDTO> getJobPostings(Long userId) {
        RecruitmentUser user = recruitmentUserReader.getEmployerByIdOrThrow(userId);
        return jobPostingRepo.findAllByEmployerIdAndStatusNot(user.id(), Status.DELETED)
                .stream()
                .map(this::toJobPostingSearchDto)
                .toList();
    }

    @Override
    @Transactional
    public void createJobPosting(JobPostingCreateDTO jobPostingCreateDTO, Long userId) {
        RecruitmentUser user = recruitmentUserReader.getEmployerByIdOrThrow(userId);
        JobPosting jobPosting = JobPosting.from(jobPostingCreateDTO, user.id(), user.name());
        jobPostingRepo.save(jobPosting);
    }

    @Override
    @Transactional
    public void updateJobPosting(JobPostingUpdateDTO jobPostingUpdateDTO, Long jobPostingId, Long userId) {
        RecruitmentUser user = recruitmentUserReader.getEmployerByIdOrThrow(userId);
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
    public void deleteJobPosting(Long jobPostingId, Long userId) {
        RecruitmentUser user = recruitmentUserReader.getEmployerByIdOrThrow(userId);
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
    public List<EmployerProjectSearchDTO> getEmployerProjects(Long userId) {
        RecruitmentUser user = recruitmentUserReader.getEmployerByIdOrThrow(userId);
        return projectPostingRepo.findAllByEmployerIdOrderByCreatedAtDesc(user.id())
                .stream()
                .map(this::toEmployerProjectSearchDto)
                .toList();
    }

    @Override
    public List<FreelancerJobPostingSearchDTO> searchJobPostingsForFreelancer(Long userId, String keyword, boolean favoritesOnly) {
        RecruitmentUser user = recruitmentUserReader.getFreelancerByIdOrThrow(userId);
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
    public void addFavoriteJobPosting(Long userId, Long jobPostingId) {
        RecruitmentUser user = recruitmentUserReader.getFreelancerByIdOrThrow(userId);
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
    public void removeFavoriteJobPosting(Long userId, Long jobPostingId) {
        RecruitmentUser user = recruitmentUserReader.getFreelancerByIdOrThrow(userId);
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

    private EmployerProjectSearchDTO toEmployerProjectSearchDto(Project project) {
        return new EmployerProjectSearchDTO(
                project.getId(),
                project.getJobPosting().getId(),
                project.getFreelancerId(),
                project.getProjectName(),
                project.getHeadcount(),
                project.getStartDate(),
                project.getEndDate(),
                project.getStatus()
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

    @Override   // 기업용
    public List<AiRecommendationResponseDTO> getRecommendedFreelancers(Long jobPostingId, Long userId) {
        JobPosting jobPosting = getJobPostingOrThrow(jobPostingId);

        validateNotDeleted(jobPosting);

        validateOwnership(jobPosting, userId);

        return recommendationEngine.recommendFreelancers(
                jobPosting.getId(),
                jobPosting.getTitle(),
                jobPosting.getDescription(),
                AiRecommendationResponseDTO.class
        );
    }

    @Override     // 프리랜서용 추천
    public List<AiRecommendationResponseDTO> getRecommendedJobsForFreelancer(Long userId) {
        // 1. 프리랜서 정보 조회
        RecruitmentUser freelancer = recruitmentUserReader.getFreelancerByIdOrThrow(userId);

        // 2. 추천에 필요한 텍스트 가공
        String skills = freelancer.skills();
        String experience = freelancer.experience();

        // 3. AI 서버 호출
        return recommendationEngine.recommendJobs(
                userId,
                skills,
                experience,
                AiRecommendationResponseDTO.class
        );
    }

    @Transactional
    public void completeProject(Long projectId) {
        Project project = projectPostingRepo.findById(projectId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PROJECT_NOT_FOUND));
        try{
            project.complete();
        } catch (IllegalStateException e) {
            throw new BusinessException(ErrorCode.PROJECT_ALREADY_COMPLETED);
        }

        Long freelancerId = project.getFreelancerId();
        RecruitmentUser freelancer = recruitmentUserReader.getFreelancerByIdOrThrow(freelancerId);
        String syncContent = String.format("프로젝트 완료: %s", project.getProjectName());

        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    recommendationEngine.syncToAiServer(
                            freelancer.id(),
                            "experience",
                            syncContent,
                            freelancer.status()
                    );
                }
            });
        }
    }
}
