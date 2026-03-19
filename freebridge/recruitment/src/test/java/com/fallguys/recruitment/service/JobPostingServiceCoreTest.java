package com.fallguys.recruitment.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fallguys.common.ai.port.RecommendationEngine;
import com.fallguys.common.exception.BusinessException;
import com.fallguys.common.exception.ErrorCode;
import com.fallguys.recruitment.api.dto.request.JobPostingCreateDTO;
import com.fallguys.recruitment.api.dto.request.JobPostingUpdateDTO;
import com.fallguys.recruitment.api.dto.response.AiRecommendationResponseDTO;
import com.fallguys.recruitment.api.dto.response.MatchedFreelancerResponseDTO;
import com.fallguys.recruitment.entity.JobPosting;
import com.fallguys.recruitment.entity.JobPostingStatus;
import com.fallguys.recruitment.entity.Project;
import com.fallguys.recruitment.entity.ProjectStatus;
import com.fallguys.recruitment.entity.Status;
import com.fallguys.recruitment.repository.JobPostingFavoriteRepo;
import com.fallguys.recruitment.repository.JobPostingRepo;
import com.fallguys.recruitment.repository.ProjectPostingRepo;
import com.fallguys.recruitment.service.port.RecruitmentUser;
import com.fallguys.recruitment.service.port.RecruitmentUserReader;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JobPostingServiceCoreTest {

    @Mock
    private JobPostingRepo jobPostingRepo;
    @Mock
    private JobPostingFavoriteRepo jobPostingFavoriteRepo;
    @Mock
    private ProjectPostingRepo projectPostingRepo;
    @Mock
    private RecruitmentUserReader recruitmentUserReader;
    @Mock
    private RecommendationEngine recommendationEngine;
    @Mock
    private RedisTemplate<String, Object> redisTemplate;
    @Mock
    private ValueOperations<String, Object> valueOperations;
    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private JobPostingServiceImpl service;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        ReflectionTestUtils.setField(service, "self", service);
    }

    @Test
    @DisplayName("[TDD] 채용공고 생성 시 고용주 정보로 저장된다")
    void createJobPosting_savesByEmployer() {
        // given
        Long userId = 1L;
        JobPostingCreateDTO request = new JobPostingCreateDTO("Backend", "desc", List.of("Java"), 1000L, 3, 2);
        when(recruitmentUserReader.getEmployerByIdOrThrow(userId))
                .thenReturn(new RecruitmentUser(userId, "employer", null, null, "ACTIVE"));

        // when
        service.createJobPosting(request, userId);

        // then
        ArgumentCaptor<JobPosting> captor = ArgumentCaptor.forClass(JobPosting.class);
        verify(jobPostingRepo, times(1)).save(captor.capture());
        assertEquals(userId, captor.getValue().getEmployerId());
        assertEquals("employer", captor.getValue().getEmployerName());
        assertEquals("Backend", captor.getValue().getTitle());
    }

    @Test
    @DisplayName("[TDD] 채용공고 수정 시 소유자가 다르면 JOB_POSTING_FORBIDDEN 예외")
    void updateJobPosting_forbiddenOwner_throws() {
        // given
        Long userId = 1L;
        Long postingId = 10L;
        JobPosting posting = posting(postingId, 99L, Status.ACTIVE);
        when(recruitmentUserReader.getEmployerByIdOrThrow(userId))
                .thenReturn(new RecruitmentUser(userId, "employer", null, null, "ACTIVE"));
        when(jobPostingRepo.findById(postingId)).thenReturn(Optional.of(posting));

        // when
        BusinessException ex = assertThrows(
                BusinessException.class,
                () -> service.updateJobPosting(new JobPostingUpdateDTO("new", null, null, null, null, null, null), postingId, userId)
        );

        // then
        assertEquals(ErrorCode.JOB_POSTING_FORBIDDEN, ex.getErrorCode());
    }

    @Test
    @DisplayName("[TDD] 채용공고 삭제 시 soft delete 된다")
    void deleteJobPosting_softDelete() {
        // given
        Long userId = 1L;
        Long postingId = 11L;
        JobPosting posting = posting(postingId, userId, Status.ACTIVE);
        when(recruitmentUserReader.getEmployerByIdOrThrow(userId))
                .thenReturn(new RecruitmentUser(userId, "employer", null, null, "ACTIVE"));
        when(jobPostingRepo.findById(postingId)).thenReturn(Optional.of(posting));

        // when
        service.deleteJobPosting(postingId, userId);

        // then
        assertEquals(Status.DELETED, posting.getStatus());
    }

    @Test
    @DisplayName("[TDD] 추천 프리랜서 조회 시 공고 소유자가 다르면 JOB_POSTING_FORBIDDEN 예외")
    void getRecommendedFreelancers_forbiddenOwner_throws() {
        // given
        Long userId = 1L;
        Long postingId = 30L;
        when(jobPostingRepo.findById(postingId)).thenReturn(Optional.of(posting(postingId, 99L, Status.ACTIVE)));

        // when
        BusinessException ex = assertThrows(
                BusinessException.class,
                () -> service.getRecommendedFreelancers(postingId, userId)
        );

        // then
        assertEquals(ErrorCode.JOB_POSTING_FORBIDDEN, ex.getErrorCode());
        verify(recommendationEngine, never()).recommendFreelancers(any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("[TDD] 프리랜서 추천공고 조회 시 skills/experience 공백은 '없음'으로 전달")
    void getRecommendedJobsForFreelancer_blankProfile_usesDefaultText() {
        // given
        Long userId = 2L;
        when(recruitmentUserReader.getFreelancerByIdOrThrow(userId))
                .thenReturn(new RecruitmentUser(userId, "freelancer", "  ", null, "ACTIVE"));
        when(recommendationEngine.recommendJobs(eq(userId), any(), any(), eq(AiRecommendationResponseDTO.class)))
                .thenReturn(List.of());

        // when
        service.getRecommendedJobsForFreelancer(userId);

        // then
        verify(recommendationEngine, times(1))
                .recommendJobs(userId, "없음", "없음", AiRecommendationResponseDTO.class);
    }

    @Test
    @DisplayName("[TDD] 프로젝트 매칭 프리랜서 목록 조회 시 같은 공고에 매칭된 프리랜서를 반환한다")
    void getMatchedFreelancers_returnsMatchedFreelancersForSameJobPosting() {
        Long employerId = 5L;
        Long projectId = 40L;
        Long jobPostingId = 100L;
        PageRequest pageable = PageRequest.of(0, 2);
        Project sourceProject = Project.create(posting(jobPostingId, employerId, Status.ACTIVE), 77L);
        Project anotherProject = Project.create(posting(jobPostingId, employerId, Status.ACTIVE), 88L);
        ReflectionTestUtils.setField(sourceProject, "id", projectId, Long.class);
        ReflectionTestUtils.setField(sourceProject, "createdAt", LocalDateTime.of(2026, 3, 8, 10, 0));
        ReflectionTestUtils.setField(anotherProject, "id", 41L, Long.class);
        ReflectionTestUtils.setField(anotherProject, "createdAt", LocalDateTime.of(2026, 3, 8, 9, 0));

        when(recruitmentUserReader.getEmployerByIdOrThrow(employerId))
                .thenReturn(new RecruitmentUser(employerId, "employer", null, null, "ACTIVE"));
        when(projectPostingRepo.findById(projectId)).thenReturn(Optional.of(sourceProject));
        when(projectPostingRepo.findAllByJobPostingIdOrderByCreatedAtDesc(jobPostingId, pageable))
                .thenReturn(new PageImpl<>(List.of(sourceProject, anotherProject), pageable, 2));
        when(recruitmentUserReader.getFreelancersByIdsOrThrow(new java.util.LinkedHashSet<>(List.of(77L, 88L))))
                .thenReturn(java.util.Map.of(
                        77L, new RecruitmentUser(77L, "kim", "[Java]", "백엔드", "ACTIVE"),
                        88L, new RecruitmentUser(88L, "lee", "[Spring]", "풀스택", "POTENTIAL")
                ));

        Page<MatchedFreelancerResponseDTO> result = service.getMatchedFreelancers(projectId, employerId, pageable);

        assertEquals(2, result.getContent().size());
        assertEquals(2L, result.getTotalElements());
        assertEquals(77L, result.getContent().get(0).freelancerId());
        assertEquals("kim", result.getContent().get(0).freelancerName());
        assertEquals(88L, result.getContent().get(1).freelancerId());
        assertEquals("lee", result.getContent().get(1).freelancerName());
        verify(recruitmentUserReader, never()).getFreelancerByIdOrThrow(any());
    }

    @Test
    @DisplayName("[TDD] 프로젝트 완료 시 이미 완료된 프로젝트면 PROJECT_ALREADY_COMPLETED 예외")
    void completeProject_alreadyCompleted_throws() {
        // given
        Long projectId = 40L;
        Long userId = 5L;
        Project project = Project.create(posting(100L, userId, Status.ACTIVE), 77L);
        ReflectionTestUtils.setField(project, "status", ProjectStatus.COMPLETED);
        when(projectPostingRepo.findById(projectId)).thenReturn(Optional.of(project));

        // when
        BusinessException ex = assertThrows(
                BusinessException.class,
                () -> service.completeProject(projectId, userId)
        );

        // then
        assertEquals(ErrorCode.PROJECT_ALREADY_COMPLETED, ex.getErrorCode());
    }

    @Test
    @DisplayName("[TDD] 계약 활성화 후 공고 마감 처리 시 postingStatus가 CLOSED로 변경된다")
    void closeJobPosting_setsPostingStatusClosed() {
        Long postingId = 50L;
        Long employerId = 5L;
        JobPosting posting = posting(postingId, employerId, Status.ACTIVE);
        ReflectionTestUtils.setField(posting, "postingStatus", JobPostingStatus.OPEN);

        when(jobPostingRepo.findById(postingId)).thenReturn(Optional.of(posting));

        service.closeJobPosting(postingId);

        assertEquals(JobPostingStatus.CLOSED, posting.getPostingStatus());
    }

    @Test
    @DisplayName("[TDD] 프로젝트 매칭 프리랜서 목록 조회 시 소유자가 다르면 JOB_POSTING_FORBIDDEN 예외")
    void getMatchedFreelancers_forbiddenOwner_throws() {
        Long employerId = 5L;
        Long projectId = 40L;
        Project project = Project.create(posting(100L, 99L, Status.ACTIVE), 77L);
        PageRequest pageable = PageRequest.of(0, 10);

        when(recruitmentUserReader.getEmployerByIdOrThrow(employerId))
                .thenReturn(new RecruitmentUser(employerId, "employer", null, null, "ACTIVE"));
        when(projectPostingRepo.findById(projectId)).thenReturn(Optional.of(project));

        BusinessException ex = assertThrows(
                BusinessException.class,
                () -> service.getMatchedFreelancers(projectId, employerId, pageable)
        );

        assertEquals(ErrorCode.JOB_POSTING_FORBIDDEN, ex.getErrorCode());
    }

    @Test
    @DisplayName("[TDD] freelancer job recommendation stores no-overlap jobs as empty result")
    void triggerJobRecommendation_filtersOutJobsWithoutTechStackOverlap() {
        Long userId = 3L;
        JobPosting noOverlapJob = posting(101L, 99L, Status.ACTIVE);
        ReflectionTestUtils.setField(noOverlapJob, "techStack", List.of("C"));

        when(recruitmentUserReader.getFreelancerByIdOrThrow(userId))
                .thenReturn(new RecruitmentUser(userId, "freelancer", "Java", "Spring", "ACTIVE"));
        when(valueOperations.setIfAbsent(anyString(), any(), any(Duration.class))).thenReturn(true);
        when(recommendationEngine.recommendJobs(eq(userId), any(), any(), eq(AiRecommendationResponseDTO.class)))
                .thenReturn(List.of(new AiRecommendationResponseDTO(101L, "C only project", 0.91, List.of(), null, null, null)));
        when(jobPostingRepo.findAllById(List.of(101L))).thenReturn(List.of(noOverlapJob));

        service.triggerJobRecommendation(userId);

        ArgumentCaptor<Object> cacheValueCaptor = ArgumentCaptor.forClass(Object.class);
        verify(valueOperations).set(eq("ai:reco:jobs:v3:" + userId), cacheValueCaptor.capture(), any(Duration.class));
        assertEquals(List.of(), cacheValueCaptor.getValue());
    }

    @Test
    @DisplayName("[TDD] freelancer job recommendation normalizes bracketed skills")
    void triggerJobRecommendation_acceptsBracketedFreelancerSkills() {
        Long userId = 4L;
        JobPosting overlapJob = posting(102L, 99L, Status.ACTIVE);
        ReflectionTestUtils.setField(overlapJob, "techStack", List.of("Java"));

        when(recruitmentUserReader.getFreelancerByIdOrThrow(userId))
                .thenReturn(new RecruitmentUser(userId, "freelancer", "[Java]", "Spring", "ACTIVE"));
        when(valueOperations.setIfAbsent(anyString(), any(), any(Duration.class))).thenReturn(true);
        when(recommendationEngine.recommendJobs(eq(userId), any(), any(), eq(AiRecommendationResponseDTO.class)))
                .thenReturn(List.of(new AiRecommendationResponseDTO(102L, "Java project", 0.95, List.of(), null, null, null)));
        when(jobPostingRepo.findAllById(List.of(102L))).thenReturn(List.of(overlapJob));

        service.triggerJobRecommendation(userId);

        ArgumentCaptor<Object> cacheValueCaptor = ArgumentCaptor.forClass(Object.class);
        verify(valueOperations).set(eq("ai:reco:jobs:v3:" + userId), cacheValueCaptor.capture(), any(Duration.class));
        @SuppressWarnings("unchecked")
        List<AiRecommendationResponseDTO> cachedResult = (List<AiRecommendationResponseDTO>) cacheValueCaptor.getValue();
        assertEquals(1, cachedResult.size());
        assertEquals(102L, cachedResult.get(0).id());
    }

    private JobPosting posting(Long id, Long employerId, Status status) {
        JobPosting posting = JobPosting.from(
                new JobPostingCreateDTO("title", "desc", List.of("Java"), 1000L, 3, 2),
                employerId,
                "employer"
        );
        ReflectionTestUtils.setField(posting, "id", id, Long.class);
        ReflectionTestUtils.setField(posting, "status", status);
        ReflectionTestUtils.setField(posting, "postingStatus", JobPostingStatus.OPEN);
        return posting;
    }
}
