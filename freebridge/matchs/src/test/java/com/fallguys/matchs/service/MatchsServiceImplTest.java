package com.fallguys.matchs.service;

import com.fallguys.common.exception.BusinessException;
import com.fallguys.common.exception.ErrorCode;
import com.fallguys.matchs.api.dto.request.ApplicationCreateRequest;
import com.fallguys.matchs.api.dto.request.ProposalCreateRequest;
import com.fallguys.matchs.entity.Application;
import com.fallguys.matchs.entity.MatchsStatus;
import com.fallguys.matchs.entity.Proposal;
import com.fallguys.matchs.repository.ApplicationRepo;
import com.fallguys.matchs.repository.ProposalRepo;
import com.fallguys.recruitment.api.dto.request.JobPostingCreateDTO;
import com.fallguys.recruitment.entity.JobPosting;
import com.fallguys.recruitment.entity.Project;
import com.fallguys.recruitment.entity.ProjectStatus;
import com.fallguys.recruitment.entity.Status;
import com.fallguys.recruitment.repository.JobPostingRepo;
import com.fallguys.recruitment.repository.ProjectPostingRepo;
import com.fallguys.recruitment.service.JobPostingService;
import com.fallguys.user.entity.Role;
import com.fallguys.user.entity.User;
import com.fallguys.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MatchsServiceImplTest {

    @Mock
    private ApplicationRepo applicationRepo;
    @Mock
    private ProposalRepo proposalRepo;
    @Mock
    private JobPostingRepo jobPostingRepo;
    @Mock
    private ProjectPostingRepo projectPostingRepo;
    @Mock
    private UserRepository userRepository;
    @Mock
    private JobPostingService jobPostingService;

    @InjectMocks
    private MatchsServiceImpl matchsService;

    @Test
    @DisplayName("[TDD] 지원 생성 시 프리랜서/공고 소유자 정보로 Application 저장")
    void createApplication_savesApplication() {
        // given
        Long freelancerId = 10L;
        Long jobPostingId = 100L;
        when(userRepository.findById(freelancerId)).thenReturn(Optional.of(user(Role.FREELANCER)));
        when(jobPostingRepo.findById(jobPostingId))
                .thenReturn(Optional.of(jobPosting(jobPostingId, 88L, Status.ACTIVE, 2, 0)));
        when(applicationRepo.save(any())).thenAnswer(invocation -> {
            Application app = invocation.getArgument(0);
            ReflectionTestUtils.setField(app, "id", 1L, Long.class);
            return app;
        });

        // when
        Long id = matchsService.createApplication(freelancerId, new ApplicationCreateRequest(jobPostingId, "hello"));

        // then
        assertEquals(1L, id);
        ArgumentCaptor<Application> captor = ArgumentCaptor.forClass(Application.class);
        verify(applicationRepo, times(1)).save(captor.capture());
        assertEquals(freelancerId, captor.getValue().getFreelancerId());
        assertEquals(88L, captor.getValue().getEmployerId());
        assertEquals(MatchsStatus.PENDING, captor.getValue().getStatus());
    }

    @Test
    @DisplayName("[TDD] 지원 생성 시 사용자 Role이 프리랜서가 아니면 ONLY_FREELANCER_ALLOWED 예외")
    void createApplication_nonFreelancer_throws() {
        // given
        Long userId = 11L;
        when(userRepository.findById(userId)).thenReturn(Optional.of(user(Role.EMPLOYER)));

        // when
        BusinessException ex = assertThrows(
                BusinessException.class,
                () -> matchsService.createApplication(userId, new ApplicationCreateRequest(1L, "msg"))
        );

        // then
        assertEquals(ErrorCode.ONLY_FREELANCER_ALLOWED, ex.getErrorCode());
    }

    @Test
    @DisplayName("[TDD] 동일한 공고에 중복 지원하면 INVALID_INPUT_VALUE 예외")
    void createApplication_duplicate_throws() {
        // given
        Long freelancerId = 10L;
        Long jobPostingId = 100L;
        when(userRepository.findById(freelancerId)).thenReturn(Optional.of(user(Role.FREELANCER)));
        when(jobPostingRepo.findById(jobPostingId))
                .thenReturn(Optional.of(jobPosting(jobPostingId, 88L, Status.ACTIVE, 2, 0)));
        when(applicationRepo.existsByJobPostingIdAndFreelancerId(jobPostingId, freelancerId)).thenReturn(true);

        // when
        BusinessException ex = assertThrows(
                BusinessException.class,
                () -> matchsService.createApplication(freelancerId, new ApplicationCreateRequest(jobPostingId, "hello"))
        );

        // then
        assertEquals(ErrorCode.INVALID_INPUT_VALUE, ex.getErrorCode());
        verify(applicationRepo, never()).save(any());
    }

    @Test
    @DisplayName("[TDD] 제안 생성 시 고용주가 공고 소유자가 아니면 JOB_POSTING_FORBIDDEN 예외")
    void createProposal_notOwner_throws() {
        // given
        Long employerId = 2L;
        ProposalCreateRequest request = new ProposalCreateRequest(200L, 3L, "msg");
        when(userRepository.findById(employerId)).thenReturn(Optional.of(user(Role.EMPLOYER)));
        when(userRepository.findById(request.freelancerId())).thenReturn(Optional.of(user(Role.FREELANCER)));
        when(jobPostingRepo.findById(request.jobPostingId()))
                .thenReturn(Optional.of(jobPosting(200L, 99L, Status.ACTIVE, 2, 0)));

        // when
        BusinessException ex = assertThrows(BusinessException.class, () -> matchsService.createProposal(employerId, request));

        // then
        assertEquals(ErrorCode.JOB_POSTING_FORBIDDEN, ex.getErrorCode());
        verify(proposalRepo, never()).save(any());
    }

    @Test
    @DisplayName("[TDD] 동일한 공고에 중복 제안하면 INVALID_INPUT_VALUE 예외")
    void createProposal_duplicate_throws() {
        // given
        Long employerId = 2L;
        ProposalCreateRequest request = new ProposalCreateRequest(200L, 3L, "msg");
        when(userRepository.findById(employerId)).thenReturn(Optional.of(user(Role.EMPLOYER)));
        when(userRepository.findById(request.freelancerId())).thenReturn(Optional.of(user(Role.FREELANCER)));
        when(jobPostingRepo.findById(request.jobPostingId()))
                .thenReturn(Optional.of(jobPosting(200L, employerId, Status.ACTIVE, 2, 0)));
        when(proposalRepo.existsByJobPostingIdAndFreelancerId(request.jobPostingId(), request.freelancerId()))
                .thenReturn(true);

        // when
        BusinessException ex = assertThrows(
                BusinessException.class,
                () -> matchsService.createProposal(employerId, request)
        );

        // then
        assertEquals(ErrorCode.INVALID_INPUT_VALUE, ex.getErrorCode());
        verify(proposalRepo, never()).save(any());
    }

    @Test
    @DisplayName("[TDD] 지원 수락 시 기존 프로젝트가 있으면 해당 프로젝트 ID를 반환")
    void acceptApplication_existingProject_returnsExistingId() {
        // given
        Long employerId = 1L;
        Long applicationId = 10L;
        Application application = Application.create(300L, 20L, employerId, "apply");
        ReflectionTestUtils.setField(application, "id", applicationId, Long.class);

        Project existingProject = Project.create(jobPosting(300L, employerId, Status.ACTIVE, 2, 0), 20L);
        ReflectionTestUtils.setField(existingProject, "id", 777L, Long.class);

        when(userRepository.findById(employerId)).thenReturn(Optional.of(user(Role.EMPLOYER)));
        when(applicationRepo.findById(applicationId)).thenReturn(Optional.of(application));
        when(jobPostingRepo.findByIdForUpdate(300L))
                .thenReturn(Optional.of(jobPosting(300L, employerId, Status.ACTIVE, 2, 0)));
        when(projectPostingRepo.findByJobPostingIdAndFreelancerId(300L, 20L)).thenReturn(Optional.of(existingProject));

        // when
        Long projectId = matchsService.acceptApplication(employerId, applicationId);

        // then
        assertEquals(777L, projectId);
        assertEquals(MatchsStatus.ACCEPTED, application.getStatus());
        verify(projectPostingRepo, never()).save(any());
    }

    @Test
    @DisplayName("[TDD] 지원 수락 시 기존 취소 프로젝트가 있으면 진행중으로 복구한다")
    void acceptApplication_cancelledProject_reopensExistingProject() {
        // given
        Long employerId = 1L;
        Long applicationId = 12L;
        Application application = Application.create(302L, 22L, employerId, "apply");
        ReflectionTestUtils.setField(application, "id", applicationId, Long.class);
        JobPosting posting = jobPosting(302L, employerId, Status.ACTIVE, 2, 0);

        Project cancelledProject = Project.create(posting, 22L);
        cancelledProject.cancel();
        ReflectionTestUtils.setField(cancelledProject, "id", 778L, Long.class);

        when(userRepository.findById(employerId)).thenReturn(Optional.of(user(Role.EMPLOYER)));
        when(applicationRepo.findById(applicationId)).thenReturn(Optional.of(application));
        when(jobPostingRepo.findByIdForUpdate(302L)).thenReturn(Optional.of(posting));
        when(projectPostingRepo.findByJobPostingIdAndFreelancerId(302L, 22L)).thenReturn(Optional.of(cancelledProject));

        // when
        Long projectId = matchsService.acceptApplication(employerId, applicationId);

        // then
        assertEquals(778L, projectId);
        assertEquals(MatchsStatus.ACCEPTED, application.getStatus());
        assertEquals(ProjectStatus.IN_PROGRESS, cancelledProject.getStatus());
        assertEquals(1, posting.getMatchedHeadcount());
        verify(projectPostingRepo, never()).save(any());
    }

    @Test
    @DisplayName("[TDD] 지원 수락 시 신규 프로젝트 생성 후 projectId를 반환")
    void acceptApplication_createsProject() {
        // given
        Long employerId = 1L;
        Long applicationId = 11L;
        Application application = Application.create(301L, 21L, employerId, "apply");
        ReflectionTestUtils.setField(application, "id", applicationId, Long.class);
        JobPosting posting = jobPosting(301L, employerId, Status.ACTIVE, 2, 0);

        when(userRepository.findById(employerId)).thenReturn(Optional.of(user(Role.EMPLOYER)));
        when(applicationRepo.findById(applicationId)).thenReturn(Optional.of(application));
        when(jobPostingRepo.findByIdForUpdate(301L)).thenReturn(Optional.of(posting));
        when(projectPostingRepo.findByJobPostingIdAndFreelancerId(301L, 21L)).thenReturn(Optional.empty());
        when(projectPostingRepo.save(any())).thenAnswer(invocation -> {
            Project project = invocation.getArgument(0);
            ReflectionTestUtils.setField(project, "id", 900L, Long.class);
            return project;
        });

        // when
        Long projectId = matchsService.acceptApplication(employerId, applicationId);

        // then
        assertEquals(900L, projectId);
        assertEquals(1, posting.getMatchedHeadcount());
    }

    @Test
    @DisplayName("[TDD] 지원 거절 시 취소 상태의 프로젝트 이력을 생성한다")
    void rejectApplication_createsCancelledProjectHistory() {
        // given
        Long employerId = 1L;
        Long applicationId = 13L;
        Application application = Application.create(303L, 23L, employerId, "apply");
        ReflectionTestUtils.setField(application, "id", applicationId, Long.class);
        JobPosting posting = jobPosting(303L, employerId, Status.ACTIVE, 2, 0);

        when(userRepository.findById(employerId)).thenReturn(Optional.of(user(Role.EMPLOYER)));
        when(applicationRepo.findById(applicationId)).thenReturn(Optional.of(application));
        when(jobPostingRepo.findByIdForUpdate(303L)).thenReturn(Optional.of(posting));
        when(projectPostingRepo.findByJobPostingIdAndFreelancerId(303L, 23L)).thenReturn(Optional.empty());
        when(projectPostingRepo.save(any())).thenAnswer(invocation -> {
            Project project = invocation.getArgument(0);
            ReflectionTestUtils.setField(project, "id", 901L, Long.class);
            return project;
        });

        // when
        Long result = matchsService.rejectApplication(employerId, applicationId);

        // then
        assertEquals(applicationId, result);
        assertEquals(MatchsStatus.REJECTED, application.getStatus());
        ArgumentCaptor<Project> captor = ArgumentCaptor.forClass(Project.class);
        verify(projectPostingRepo).save(captor.capture());
        assertEquals(ProjectStatus.CANCELLED, captor.getValue().getStatus());
        assertEquals(0, posting.getMatchedHeadcount());
    }

    @Test
    @DisplayName("[TDD] 제안 거절 시 상태가 PENDING이 아니면 INVALID_INPUT_VALUE 예외")
    void rejectProposal_notPending_throws() {
        // given
        Long freelancerId = 33L;
        Long proposalId = 44L;
        Proposal proposal = Proposal.create(1L, freelancerId, 77L, "msg");
        proposal.accept();
        ReflectionTestUtils.setField(proposal, "id", proposalId, Long.class);
        when(userRepository.findById(freelancerId)).thenReturn(Optional.of(user(Role.FREELANCER)));
        when(proposalRepo.findById(proposalId)).thenReturn(Optional.of(proposal));

        // when
        BusinessException ex = assertThrows(BusinessException.class, () -> matchsService.rejectProposal(freelancerId, proposalId));

        // then
        assertEquals(ErrorCode.INVALID_INPUT_VALUE, ex.getErrorCode());
    }

    @Test
    @DisplayName("[TDD] 제안 거절 시 취소 상태의 프로젝트 이력을 생성한다")
    void rejectProposal_createsCancelledProjectHistory() {
        // given
        Long freelancerId = 33L;
        Long proposalId = 45L;
        Proposal proposal = Proposal.create(304L, freelancerId, 77L, "msg");
        ReflectionTestUtils.setField(proposal, "id", proposalId, Long.class);
        JobPosting posting = jobPosting(304L, 77L, Status.ACTIVE, 2, 0);

        when(userRepository.findById(freelancerId)).thenReturn(Optional.of(user(Role.FREELANCER)));
        when(proposalRepo.findById(proposalId)).thenReturn(Optional.of(proposal));
        when(jobPostingRepo.findByIdForUpdate(304L)).thenReturn(Optional.of(posting));
        when(projectPostingRepo.findByJobPostingIdAndFreelancerId(304L, freelancerId)).thenReturn(Optional.empty());
        when(projectPostingRepo.save(any())).thenAnswer(invocation -> {
            Project project = invocation.getArgument(0);
            ReflectionTestUtils.setField(project, "id", 902L, Long.class);
            return project;
        });

        // when
        Long result = matchsService.rejectProposal(freelancerId, proposalId);

        // then
        assertEquals(proposalId, result);
        assertEquals(MatchsStatus.REJECTED, proposal.getStatus());
        ArgumentCaptor<Project> captor = ArgumentCaptor.forClass(Project.class);
        verify(projectPostingRepo).save(captor.capture());
        assertEquals(ProjectStatus.CANCELLED, captor.getValue().getStatus());
        assertEquals(0, posting.getMatchedHeadcount());
    }

    @Test
    @DisplayName("[TDD] 고용주 지원 상세 조회 시 소유자가 다르면 JOB_POSTING_FORBIDDEN 예외")
    void getEmployerApplication_forbidden_throws() {
        // given
        Long employerId = 1L;
        Long applicationId = 55L;
        Application application = Application.create(10L, 20L, 99L, "msg");
        ReflectionTestUtils.setField(application, "id", applicationId, Long.class);
        when(userRepository.findById(employerId)).thenReturn(Optional.of(user(Role.EMPLOYER)));
        when(applicationRepo.findById(applicationId)).thenReturn(Optional.of(application));

        // when
        BusinessException ex = assertThrows(
                BusinessException.class,
                () -> matchsService.getEmployerApplication(employerId, applicationId)
        );

        // then
        assertEquals(ErrorCode.JOB_POSTING_FORBIDDEN, ex.getErrorCode());
    }

    private User user(Role role) {
        return User.builder()
                .email(role.name().toLowerCase() + "@test.com")
                .password("pw")
                .name("name")
                .role(role)
                .termsAgreed(true)
                .privacyAgreed(true)
                .build();
    }

    private JobPosting jobPosting(Long id, Long employerId, Status status, int headcount, int matched) {
        JobPosting posting = JobPosting.from(
                new JobPostingCreateDTO("title", "desc", java.util.List.of("Java"), 1000L, 3, headcount),
                employerId,
                "employer"
        );
        ReflectionTestUtils.setField(posting, "id", id, Long.class);
        ReflectionTestUtils.setField(posting, "status", status);
        ReflectionTestUtils.setField(posting, "matchedHeadcount", matched);
        return posting;
    }
}
