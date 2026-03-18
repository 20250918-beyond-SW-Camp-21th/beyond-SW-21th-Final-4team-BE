package com.fallguys.matchs.service;

import com.fallguys.common.exception.BusinessException;
import com.fallguys.common.exception.ErrorCode;
import com.fallguys.matchs.api.dto.request.ApplicationCreateRequest;
import com.fallguys.matchs.api.dto.request.ProposalCreateRequest;
import com.fallguys.matchs.api.dto.response.ApplicationResponseDTO;
import com.fallguys.matchs.api.dto.response.ProposalResponseDTO;
import com.fallguys.matchs.entity.Application;
import com.fallguys.matchs.entity.MatchsStatus;
import com.fallguys.matchs.entity.Proposal;
import com.fallguys.matchs.repository.ApplicationRepo;
import com.fallguys.matchs.repository.ProposalRepo;
import com.fallguys.recruitment.entity.JobPosting;
import com.fallguys.recruitment.entity.JobPostingStatus;
import com.fallguys.recruitment.entity.Project;
import com.fallguys.recruitment.entity.ProjectStatus;
import com.fallguys.recruitment.entity.Status;
import com.fallguys.recruitment.repository.JobPostingRepo;
import com.fallguys.recruitment.repository.ProjectPostingRepo;
import com.fallguys.recruitment.service.JobPostingService;
import com.fallguys.user.entity.Role;
import com.fallguys.user.entity.User;
import com.fallguys.user.repository.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MatchsServiceImpl implements MatchsService {

    private static final String EMPLOYER_PROJECT_LIST_KEY_PREFIX = "employer:project:list:";
    private static final String EMPLOYER_PROJECT_APPLICANTS_KEY_PREFIX = "employer:project:applicants:";
    private static final String FREELANCER_PROJECT_STATS_KEY_PREFIX = "freelancer:project:stats:";
    private static final String FREELANCER_PROJECT_LIST_KEY_PREFIX = "freelancer:project:list:";
    private static final DateTimeFormatter ISO_SECONDS_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
    private static final int APPLICANT_COUNT_QUERY_CHUNK_SIZE = 500;

    private final ApplicationRepo applicationRepo;
    private final ProposalRepo proposalRepo;
    private final JobPostingRepo jobPostingRepo;
    private final ProjectPostingRepo projectPostingRepo;
    private final JobPostingService jobPostingService;
    private final UserRepository userRepository;
    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    @Transactional
    public Long createApplication(Long freelancerId, ApplicationCreateRequest request) {
        getUserByRoleOrThrow(freelancerId, Role.FREELANCER);
        JobPosting jobPosting = getOpenJobPostingOrThrow(request.jobPostingId());
        validateNoDuplicateApplication(request.jobPostingId(), freelancerId);

        Application application = Application.create(
                jobPosting.getId(),
                freelancerId,
                jobPosting.getEmployerId(),
                request.message()
        );

        Long applicationId = saveApplication(application).getId();
        runAfterCommitSafely(() -> {
            refreshEmployerProjectStats(jobPosting.getEmployerId());
            refreshEmployerProjectList(jobPosting.getEmployerId());
            refreshEmployerProjectApplicants(applicationId);
            refreshFreelancerProjectStats(freelancerId);
            refreshFreelancerProjectList(freelancerId);
        });
        return applicationId;
    }

    @Override
    @Transactional
    public Long createProposal(Long employerId, ProposalCreateRequest request) {
        getUserByRoleOrThrow(employerId, Role.EMPLOYER);
        getUserByRoleOrThrow(request.freelancerId(), Role.FREELANCER);

        JobPosting jobPosting = getOpenJobPostingOrThrow(request.jobPostingId());
        if (!jobPosting.getEmployerId().equals(employerId)) {
            throw new BusinessException(ErrorCode.JOB_POSTING_FORBIDDEN);
        }
        validateNoDuplicateProposal(request.jobPostingId(), request.freelancerId());

        Proposal proposal = Proposal.create(
                request.jobPostingId(),
                request.freelancerId(),
                employerId,
                request.message()
        );

        Long proposalId = saveProposal(proposal).getId();
        runAfterCommitSafely(() -> {
            refreshEmployerProjectStats(employerId);
            refreshEmployerProjectList(employerId);
            refreshFreelancerProjectStats(request.freelancerId());
            refreshFreelancerProjectList(request.freelancerId());
        });
        return proposalId;
    }

    @Override
    @Transactional
    public Long acceptApplication(Long employerId, Long applicationId) {
        getUserByRoleOrThrow(employerId, Role.EMPLOYER);
        Application application = getApplicationOrThrow(applicationId);

        validateEmployerOwner(application.getEmployerId(), employerId);
        validatePending(application.getStatus());

        application.accept();
        Long projectId = createProjectIfAbsent(application.getJobPostingId(), application.getFreelancerId());
        runAfterCommitSafely(() -> {
            jobPostingService.refreshEmployerRecruitmentCaches(application.getEmployerId());
            refreshEmployerProjectStats(application.getEmployerId());
            refreshEmployerProjectList(application.getEmployerId());
            refreshEmployerProjectApplicants(application.getId());
            refreshFreelancerProjectStats(application.getFreelancerId());
            refreshFreelancerProjectList(application.getFreelancerId());
        });
        return projectId;
    }

    @Override
    @Transactional
    public Long acceptProposal(Long freelancerId, Long proposalId) {
        getUserByRoleOrThrow(freelancerId, Role.FREELANCER);
        Proposal proposal = getProposalOrThrow(proposalId);

        validateFreelancerOwner(proposal.getFreelancerId(), freelancerId);
        validatePending(proposal.getStatus());

        proposal.accept();
        Long projectId = createProjectIfAbsent(proposal.getJobPostingId(), freelancerId);
        runAfterCommitSafely(() -> {
            jobPostingService.refreshEmployerRecruitmentCaches(proposal.getEmployerId());
            refreshEmployerProjectStats(proposal.getEmployerId());
            refreshEmployerProjectList(proposal.getEmployerId());
            refreshFreelancerProjectStats(freelancerId);
            refreshFreelancerProjectList(freelancerId);
        });
        return projectId;
    }

    @Override
    @Transactional
    public Long rejectApplication(Long employerId, Long applicationId) {
        getUserByRoleOrThrow(employerId, Role.EMPLOYER);
        Application application = getApplicationOrThrow(applicationId);

        validateEmployerOwner(application.getEmployerId(), employerId);
        validatePending(application.getStatus());

        application.reject();
        ensureRejectedProjectHistory(application.getJobPostingId(), application.getFreelancerId());
        runAfterCommitSafely(() -> {
            refreshEmployerProjectStats(application.getEmployerId());
            refreshEmployerProjectList(application.getEmployerId());
            refreshEmployerProjectApplicants(application.getId());
            refreshFreelancerProjectStats(application.getFreelancerId());
            refreshFreelancerProjectList(application.getFreelancerId());
        });
        return application.getId();
    }

    @Override
    @Transactional
    public Long rejectProposal(Long freelancerId, Long proposalId) {
        getUserByRoleOrThrow(freelancerId, Role.FREELANCER);
        Proposal proposal = getProposalOrThrow(proposalId);

        validateFreelancerOwner(proposal.getFreelancerId(), freelancerId);
        validatePending(proposal.getStatus());

        proposal.reject();
        ensureRejectedProjectHistory(proposal.getJobPostingId(), freelancerId);
        runAfterCommitSafely(() -> {
            refreshEmployerProjectStats(proposal.getEmployerId());
            refreshEmployerProjectList(proposal.getEmployerId());
            refreshFreelancerProjectStats(freelancerId);
            refreshFreelancerProjectList(freelancerId);
        });
        return proposal.getId();
    }

    @Override
    public Page<ApplicationResponseDTO> getEmployerApplications(Long employerId, Pageable pageable) {
        getUserByRoleOrThrow(employerId, Role.EMPLOYER);
        return applicationRepo.findAllByEmployerIdOrderByCreatedAtDesc(employerId, pageable)
                .map(this::toApplicationResponse);
    }

    @Override
    public ApplicationResponseDTO getEmployerApplication(Long employerId, Long applicationId) {
        getUserByRoleOrThrow(employerId, Role.EMPLOYER);
        Application application = getApplicationOrThrow(applicationId);
        validateEmployerOwner(application.getEmployerId(), employerId);
        return toApplicationResponse(application);
    }

    @Override
    public Page<ProposalResponseDTO> getEmployerProposals(Long employerId, Pageable pageable) {
        getUserByRoleOrThrow(employerId, Role.EMPLOYER);
        return proposalRepo.findAllByEmployerIdOrderByCreatedAtDesc(employerId, pageable)
                .map(this::toProposalResponse);
    }

    @Override
    public ProposalResponseDTO getEmployerProposal(Long employerId, Long proposalId) {
        getUserByRoleOrThrow(employerId, Role.EMPLOYER);
        Proposal proposal = getProposalOrThrow(proposalId);
        validateEmployerOwner(proposal.getEmployerId(), employerId);
        return toProposalResponse(proposal);
    }

    @Override
    public Page<ApplicationResponseDTO> getFreelancerApplications(Long freelancerId, Pageable pageable) {
        getUserByRoleOrThrow(freelancerId, Role.FREELANCER);
        return applicationRepo.findAllByFreelancerIdOrderByCreatedAtDesc(freelancerId, pageable)
                .map(this::toApplicationResponse);
    }

    @Override
    public ApplicationResponseDTO getFreelancerApplication(Long freelancerId, Long applicationId) {
        getUserByRoleOrThrow(freelancerId, Role.FREELANCER);
        Application application = getApplicationOrThrow(applicationId);
        validateFreelancerOwner(application.getFreelancerId(), freelancerId);
        return toApplicationResponse(application);
    }

    @Override
    public Page<ProposalResponseDTO> getFreelancerProposals(Long freelancerId, Pageable pageable) {
        getUserByRoleOrThrow(freelancerId, Role.FREELANCER);
        return proposalRepo.findAllByFreelancerIdOrderByCreatedAtDesc(freelancerId, pageable)
                .map(this::toProposalResponse);
    }

    @Override
    public ProposalResponseDTO getFreelancerProposal(Long freelancerId, Long proposalId) {
        getUserByRoleOrThrow(freelancerId, Role.FREELANCER);
        Proposal proposal = getProposalOrThrow(proposalId);
        validateFreelancerOwner(proposal.getFreelancerId(), freelancerId);
        return toProposalResponse(proposal);
    }

    private Long createProjectIfAbsent(Long jobPostingId, Long freelancerId) {
        JobPosting jobPosting = getOpenJobPostingForUpdateOrThrow(jobPostingId);
        Project existing = projectPostingRepo.findByJobPostingIdAndFreelancerId(jobPostingId, freelancerId).orElse(null);
        if (existing != null) {
            if (existing.getStatus() == ProjectStatus.CANCELLED) {
                if (jobPosting.isRecruitmentFull()) {
                    throw new BusinessException(ErrorCode.JOB_POSTING_HEADCOUNT_FULL);
                }
                existing.reopen();
                jobPosting.matchFreelancer();
            }
            return existing.getId();
        }

        if (jobPosting.isRecruitmentFull()) {
            throw new BusinessException(ErrorCode.JOB_POSTING_HEADCOUNT_FULL);
        }

        Project project = Project.create(jobPosting, freelancerId);
        Long projectId = projectPostingRepo.save(project).getId();
        jobPosting.matchFreelancer();
        return projectId;
    }

    private Long ensureRejectedProjectHistory(Long jobPostingId, Long freelancerId) {
        JobPosting jobPosting = getJobPostingForProjectHistoryOrThrow(jobPostingId);
        Project existing = projectPostingRepo.findByJobPostingIdAndFreelancerId(jobPostingId, freelancerId).orElse(null);
        if (existing != null) {
            return existing.getId();
        }

        Project project = Project.create(jobPosting, freelancerId);
        project.cancel();
        return projectPostingRepo.save(project).getId();
    }

    private Application getApplicationOrThrow(Long applicationId) {
        return applicationRepo.findById(applicationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_INPUT_VALUE));
    }

    private Proposal getProposalOrThrow(Long proposalId) {
        return proposalRepo.findById(proposalId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_INPUT_VALUE));
    }

    private JobPosting getOpenJobPostingOrThrow(Long jobPostingId) {
        JobPosting jobPosting = jobPostingRepo.findById(jobPostingId)
                .orElseThrow(() -> new BusinessException(ErrorCode.JOB_POSTING_NOT_FOUND));
        if (jobPosting.getStatus() == Status.DELETED) {
            throw new BusinessException(ErrorCode.JOB_POSTING_ALREADY_DELETED);
        }
        return jobPosting;
    }

    private JobPosting getOpenJobPostingForUpdateOrThrow(Long jobPostingId) {
        JobPosting jobPosting = jobPostingRepo.findByIdForUpdate(jobPostingId)
                .orElseThrow(() -> new BusinessException(ErrorCode.JOB_POSTING_NOT_FOUND));
        if (jobPosting.getStatus() == Status.DELETED) {
            throw new BusinessException(ErrorCode.JOB_POSTING_ALREADY_DELETED);
        }
        return jobPosting;
    }

    private JobPosting getJobPostingForProjectHistoryOrThrow(Long jobPostingId) {
        return jobPostingRepo.findByIdForUpdate(jobPostingId)
                .orElseThrow(() -> new BusinessException(ErrorCode.JOB_POSTING_NOT_FOUND));
    }

    private void validateNoDuplicateApplication(Long jobPostingId, Long freelancerId) {
        if (applicationRepo.existsByJobPostingIdAndFreelancerId(jobPostingId, freelancerId)) {
            throw duplicateMatchRequest();
        }
    }

    private void validateNoDuplicateProposal(Long jobPostingId, Long freelancerId) {
        if (proposalRepo.existsByJobPostingIdAndFreelancerId(jobPostingId, freelancerId)) {
            throw duplicateMatchRequest();
        }
    }

    private Application saveApplication(Application application) {
        try {
            return applicationRepo.save(application);
        } catch (DataIntegrityViolationException e) {
            throw duplicateMatchRequest();
        }
    }

    private Proposal saveProposal(Proposal proposal) {
        try {
            return proposalRepo.save(proposal);
        } catch (DataIntegrityViolationException e) {
            throw duplicateMatchRequest();
        }
    }

    private BusinessException duplicateMatchRequest() {
        return new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
    }

    private User getUserByRoleOrThrow(Long userId, Role role) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (user.getRole() != role) {
            if (role == Role.EMPLOYER) {
                throw new BusinessException(ErrorCode.ONLY_EMPLOYER_ALLOWED);
            }
            throw new BusinessException(ErrorCode.ONLY_FREELANCER_ALLOWED);
        }
        return user;
    }

    private void validateEmployerOwner(Long ownerEmployerId, Long employerId) {
        if (!ownerEmployerId.equals(employerId)) {
            throw new BusinessException(ErrorCode.JOB_POSTING_FORBIDDEN);
        }
    }

    private void validateFreelancerOwner(Long ownerFreelancerId, Long freelancerId) {
        if (!ownerFreelancerId.equals(freelancerId)) {
            throw new BusinessException(ErrorCode.JOB_POSTING_FORBIDDEN);
        }
    }

    private void validatePending(MatchsStatus status) {
        if (status != MatchsStatus.PENDING) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
    }

    private ApplicationResponseDTO toApplicationResponse(Application application) {
        return new ApplicationResponseDTO(
                application.getId(),
                application.getJobPostingId(),
                application.getFreelancerId(),
                application.getEmployerId(),
                application.getMessage(),
                application.getStatus(),
                application.getCreatedAt()
        );
    }

    private ProposalResponseDTO toProposalResponse(Proposal proposal) {
        return new ProposalResponseDTO(
                proposal.getId(),
                proposal.getJobPostingId(),
                proposal.getFreelancerId(),
                proposal.getEmployerId(),
                proposal.getMessage(),
                proposal.getStatus(),
                proposal.getCreatedAt()
        );
    }

    private void runAfterCommit(Runnable task) {
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    task.run();
                }
            });
            return;
        }
        task.run();
    }

    private void runAfterCommitSafely(Runnable task) {
        runAfterCommit(() -> {
            try {
                task.run();
            } catch (RuntimeException e) {
                log.warn("Failed to refresh mypage redis payload after commit", e);
            }
        });
    }

    private void refreshEmployerProjectStats(Long employerId) {
        jobPostingService.refreshEmployerProjectStatsCache(employerId);
    }

    private void refreshEmployerProjectList(Long employerId) {
        List<JobPosting> postings = orEmpty(jobPostingRepo.findAllByEmployerIdAndStatusNot(employerId, Status.DELETED))
                .stream()
                .sorted(Comparator.comparing(JobPosting::getCreatedAt).reversed())
                .toList();

        List<Long> postingIds = postings.stream()
                .map(JobPosting::getId)
                .filter(Objects::nonNull)
                .toList();
        Map<Long, Integer> applicantCountsByPostingId = loadApplicantCountsByPostingIds(postingIds);

        List<Map<String, Object>> payload = new ArrayList<>(postings.size());
        for (JobPosting posting : postings) {
            Map<String, Object> item = new HashMap<>();
            item.put("projectId", posting.getId());
            item.put("title", posting.getTitle());
            item.put("status", toEmployerProjectStatus(posting.getPostingStatus()));
            item.put("applicantCount", applicantCountsByPostingId.getOrDefault(posting.getId(), 0));
            item.put("createdAt", formatIsoSeconds(posting.getCreatedAt()));

            // Job posting duration is an estimated project period in months, not a recruitment deadline.
            item.put("deadline", null);
            payload.add(item);
        }

        writeRedisValue(EMPLOYER_PROJECT_LIST_KEY_PREFIX + employerId, payload);
    }

    private Map<Long, Integer> loadApplicantCountsByPostingIds(List<Long> postingIds) {
        Map<Long, Integer> countsByPostingId = new HashMap<>();
        if (postingIds.isEmpty()) {
            return countsByPostingId;
        }

        for (int start = 0; start < postingIds.size(); start += APPLICANT_COUNT_QUERY_CHUNK_SIZE) {
            int end = Math.min(start + APPLICANT_COUNT_QUERY_CHUNK_SIZE, postingIds.size());
            List<Long> chunk = postingIds.subList(start, end);
            orEmpty(applicationRepo.countApplicantsByJobPostingIds(chunk))
                    .forEach(result -> {
                        Long jobPostingId = result.getJobPostingId();
                        if (jobPostingId == null) {
                            return;
                        }
                        Long applicantCount = result.getApplicantCount();
                        int normalizedCount = applicantCount == null ? 0 : Math.toIntExact(applicantCount);
                        countsByPostingId.merge(jobPostingId, normalizedCount, Integer::sum);
                    });
        }
        return countsByPostingId;
    }

    private void refreshEmployerProjectApplicants(Long applicationId) {
        applicationRepo.findApplicantStatusProjectionById(applicationId)
                .ifPresent(this::upsertEmployerProjectApplicant);
    }

    private void upsertEmployerProjectApplicant(ApplicationRepo.ApplicantStatusProjection applicant) {
        Long projectId = applicant.getJobPostingId();
        Long freelancerId = applicant.getFreelancerId();
        if (projectId == null || freelancerId == null) {
            return;
        }

        String redisKey = EMPLOYER_PROJECT_APPLICANTS_KEY_PREFIX + projectId;
        List<Map<String, Object>> payload = readEmployerProjectApplicantsPayload(redisKey);

        Map<String, Object> updatedItem = new HashMap<>();
        updatedItem.put("freelancerId", freelancerId);
        updatedItem.put("applyStatus", toEmployerApplicantStatus(applicant.getStatus()));

        boolean updated = false;
        for (int i = 0; i < payload.size(); i++) {
            Map<String, Object> current = payload.get(i);
            if (current == null) {
                continue;
            }
            Object currentFreelancerId = current.get("freelancerId");
            if (currentFreelancerId == null) {
                continue;
            }
            if (freelancerId.equals(parseLongSafely(currentFreelancerId))) {
                payload.set(i, updatedItem);
                updated = true;
                break;
            }
        }

        if (!updated) {
            payload.add(0, updatedItem);
        }

        writeRedisValue(redisKey, payload);
    }

    private List<Map<String, Object>> readEmployerProjectApplicantsPayload(String redisKey) {
        try {
            Object raw = redisTemplate.opsForValue().get(redisKey);
            if (!(raw instanceof List<?> list)) {
                return new ArrayList<>();
            }
            List<Map<String, Object>> payload = new ArrayList<>(list.size());
            for (Object entry : list) {
                if (entry instanceof Map<?, ?> rawMap) {
                    Map<String, Object> item = new HashMap<>();
                    rawMap.forEach((key, value) -> item.put(String.valueOf(key), value));
                    payload.add(item);
                }
            }
            return payload;
        } catch (RuntimeException e) {
            log.warn("Failed to read employer project applicants payload. key={}", redisKey, e);
            return new ArrayList<>();
        }
    }

    private void refreshFreelancerProjectStats(Long freelancerId) {
        int appliedProjects = Math.toIntExact(
                applicationRepo.countByFreelancerId(freelancerId)
                        + proposalRepo.countByFreelancerId(freelancerId)
        );
        int inProgressProjects = Math.toIntExact(
                projectPostingRepo.countByFreelancerIdAndStatus(freelancerId, ProjectStatus.IN_PROGRESS)
        );
        int completedProjects = Math.toIntExact(
                projectPostingRepo.countByFreelancerIdAndStatus(freelancerId, ProjectStatus.COMPLETED)
        );

        Map<String, Object> payload = new HashMap<>();
        payload.put("appliedProjects", appliedProjects);
        payload.put("inProgressProjects", inProgressProjects);
        payload.put("completedProjects", completedProjects);

        writeRedisValue(FREELANCER_PROJECT_STATS_KEY_PREFIX + freelancerId, payload);
    }

    private void refreshFreelancerProjectList(Long freelancerId) {
        List<Project> projects = orEmpty(projectPostingRepo.findAllByFreelancerIdOrderByCreatedAtDesc(freelancerId));

        Set<Long> postingIds = new LinkedHashSet<>();
        projects.stream()
                .map(Project::getJobPosting)
                .filter(Objects::nonNull)
                .map(JobPosting::getId)
                .filter(Objects::nonNull)
                .forEach(postingIds::add);

        Map<Long, JobPosting> postingsById = new HashMap<>();
        if (!postingIds.isEmpty()) {
            jobPostingRepo.findAllById(postingIds).forEach(posting -> postingsById.put(posting.getId(), posting));
        }

        List<Map<String, Object>> payload = new ArrayList<>(projects.size());
        for (Project project : projects) {
            JobPosting posting = project.getJobPosting();
            if (posting != null && posting.getId() != null) {
                posting = postingsById.getOrDefault(posting.getId(), posting);
            }
            payload.add(toFreelancerProjectItem(project, posting));
        }

        writeRedisValue(FREELANCER_PROJECT_LIST_KEY_PREFIX + freelancerId, payload);
    }

    private Map<String, Object> toFreelancerProjectItem(Project project, JobPosting posting) {
        Map<String, Object> item = new HashMap<>();
        item.put("projectId", project.getId());
        item.put("title", posting != null ? posting.getTitle() : project.getProjectName());
        item.put("employerName", posting != null ? posting.getEmployerName() : null);
        item.put("projectStatus", toFreelancerProjectStatus(project.getStatus()));
        item.put("description", posting != null ? posting.getDescription() : null);
        item.put("budget", posting != null ? posting.getBudget() : null);
        item.put("techStack", posting != null ? List.copyOf(posting.getTechStack()) : List.of());
        item.put("startDate", project.getStartDate() != null ? project.getStartDate().toString() : null);
        item.put("endDate", project.getEndDate() != null ? project.getEndDate().toString() : null);
        return item;
    }

    private String toEmployerProjectStatus(JobPostingStatus status) {
        if (status == null) {
            return "모집중";
        }
        return switch (status) {
            case OPEN -> "모집중";
            case IN_PROGRESS -> "진행중";
            case COMPLETED -> "완료";
            case CLOSED -> "마감";
        };
    }

    private String toEmployerApplicantStatus(MatchsStatus status) {
        if (status == null) {
            return "검토중";
        }
        return switch (status) {
            case PENDING -> "검토중";
            case ACCEPTED -> "합격";
            case REJECTED -> "거절";
        };
    }

    private String toFreelancerProjectStatus(ProjectStatus status) {
        if (status == null) {
            return "IN_PROGRESS";
        }
        return switch (status) {
            case IN_PROGRESS -> "IN_PROGRESS";
            case COMPLETED -> "COMPLETED";
            case CANCELLED -> "CANCELED";
        };
    }

    private String formatIsoSeconds(LocalDateTime dateTime) {
        if (dateTime == null) {
            return null;
        }
        return dateTime.format(ISO_SECONDS_FORMATTER);
    }

    private Long parseLongSafely(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value != null) {
            try {
                return Long.parseLong(value.toString());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private void writeRedisValue(String key, Object value) {
        if (redisTemplate == null) {
            return;
        }
        try {
            redisTemplate.opsForValue().set(key, value);
        } catch (RuntimeException e) {
            log.warn("Failed to write mypage redis payload. key={}", key, e);
        }
    }

    private <T> List<T> orEmpty(List<T> list) {
        return list == null ? List.of() : list;
    }
}
