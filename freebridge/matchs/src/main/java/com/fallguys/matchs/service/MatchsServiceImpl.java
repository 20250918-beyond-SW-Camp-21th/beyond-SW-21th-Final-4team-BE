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
import com.fallguys.recruitment.entity.Project;
import com.fallguys.recruitment.entity.Status;
import com.fallguys.recruitment.repository.JobPostingRepo;
import com.fallguys.recruitment.repository.ProjectPostingRepo;
import com.fallguys.user.entity.Role;
import com.fallguys.user.entity.User;
import com.fallguys.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MatchsServiceImpl implements MatchsService {

    private final ApplicationRepo applicationRepo;
    private final ProposalRepo proposalRepo;
    private final JobPostingRepo jobPostingRepo;
    private final ProjectPostingRepo projectPostingRepo;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public Long createApplication(Long freelancerId, ApplicationCreateRequest request) {
        getUserByRoleOrThrow(freelancerId, Role.FREELANCER);
        JobPosting jobPosting = getOpenJobPostingOrThrow(request.jobPostingId());

        Application application = Application.create(
                jobPosting.getId(),
                freelancerId,
                jobPosting.getEmployerId(),
                request.message()
        );

        return applicationRepo.save(application).getId();
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

        Proposal proposal = Proposal.create(
                request.jobPostingId(),
                request.freelancerId(),
                employerId,
                request.message()
        );

        return proposalRepo.save(proposal).getId();
    }

    @Override
    @Transactional
    public Long acceptApplication(Long employerId, Long applicationId) {
        getUserByRoleOrThrow(employerId, Role.EMPLOYER);
        Application application = getApplicationOrThrow(applicationId);

        validateEmployerOwner(application.getEmployerId(), employerId);
        validatePending(application.getStatus());

        application.accept();
        return createProjectIfAbsent(application.getJobPostingId(), application.getFreelancerId());
    }

    @Override
    @Transactional
    public Long acceptProposal(Long freelancerId, Long proposalId) {
        getUserByRoleOrThrow(freelancerId, Role.FREELANCER);
        Proposal proposal = getProposalOrThrow(proposalId);

        validateFreelancerOwner(proposal.getFreelancerId(), freelancerId);
        validatePending(proposal.getStatus());

        proposal.accept();
        return createProjectIfAbsent(proposal.getJobPostingId(), freelancerId);
    }

    @Override
    @Transactional
    public Long rejectApplication(Long employerId, Long applicationId) {
        getUserByRoleOrThrow(employerId, Role.EMPLOYER);
        Application application = getApplicationOrThrow(applicationId);

        validateEmployerOwner(application.getEmployerId(), employerId);
        validatePending(application.getStatus());

        application.reject();
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
}
