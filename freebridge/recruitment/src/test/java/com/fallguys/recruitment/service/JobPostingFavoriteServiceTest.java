package com.fallguys.recruitment.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fallguys.common.ai.port.RecommendationEngine;
import com.fallguys.common.exception.BusinessException;
import com.fallguys.common.exception.ErrorCode;
import com.fallguys.recruitment.api.dto.request.JobPostingCreateDTO;
import com.fallguys.recruitment.api.dto.response.FreelancerJobPostingSearchDTO;
import com.fallguys.recruitment.entity.JobPosting;
import com.fallguys.recruitment.entity.JobPostingFavorite;
import com.fallguys.recruitment.entity.JobPostingStatus;
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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JobPostingFavoriteServiceTest {

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
    private JobPostingServiceImpl jobPostingService;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    @DisplayName("[TDD] 관심 공고 등록 시 freelancer 기준으로 즐겨찾기가 저장된다")
    void addFavoriteJobPosting_savesFavorite() {
        // given
        Long userId = 10L;
        Long jobPostingId = 101L;
        when(recruitmentUserReader.getFreelancerByIdOrThrow(userId))
                .thenReturn(new RecruitmentUser(77L, "freelancer", null, null, "ACTIVE"));
        when(jobPostingRepo.findById(jobPostingId))
                .thenReturn(Optional.of(createJobPosting(jobPostingId, "Spring", Status.ACTIVE, JobPostingStatus.OPEN)));

        // when
        jobPostingService.addFavoriteJobPosting(userId, jobPostingId);

        // then
        ArgumentCaptor<JobPostingFavorite> captor = ArgumentCaptor.forClass(JobPostingFavorite.class);
        verify(jobPostingFavoriteRepo, times(1)).save(captor.capture());
        assertEquals(77L, captor.getValue().getFreelancerId());
        assertEquals(jobPostingId, captor.getValue().getJobPostingId());
    }

    @Test
    @DisplayName("[TDD] 동일 관심 공고 중복 등록 시 예외 없이 no-op 처리된다")
    void addFavoriteJobPosting_duplicateFavorite_isNoOp() {
        // given
        Long userId = 11L;
        Long jobPostingId = 102L;
        when(recruitmentUserReader.getFreelancerByIdOrThrow(userId))
                .thenReturn(new RecruitmentUser(88L, "freelancer", null, null, "ACTIVE"));
        when(jobPostingRepo.findById(jobPostingId))
                .thenReturn(Optional.of(createJobPosting(jobPostingId, "Java", Status.ACTIVE, JobPostingStatus.OPEN)));
        when(jobPostingFavoriteRepo.save(any())).thenThrow(new DataIntegrityViolationException("duplicate"));

        // when & then
        assertDoesNotThrow(() -> jobPostingService.addFavoriteJobPosting(userId, jobPostingId));
    }

    @Test
    @DisplayName("[TDD] 삭제된 공고는 관심 공고 등록 시 JOB_POSTING_ALREADY_DELETED 예외를 던진다")
    void addFavoriteJobPosting_deletedPosting_throwsBusinessException() {
        // given
        Long userId = 12L;
        Long jobPostingId = 103L;
        when(recruitmentUserReader.getFreelancerByIdOrThrow(userId))
                .thenReturn(new RecruitmentUser(99L, "freelancer", null, null, "ACTIVE"));
        when(jobPostingRepo.findById(jobPostingId))
                .thenReturn(Optional.of(createJobPosting(jobPostingId, "Kotlin", Status.DELETED, JobPostingStatus.CLOSED)));

        // when
        BusinessException ex = assertThrows(
                BusinessException.class,
                () -> jobPostingService.addFavoriteJobPosting(userId, jobPostingId)
        );

        // then
        assertEquals(ErrorCode.JOB_POSTING_ALREADY_DELETED, ex.getErrorCode());
        verify(jobPostingFavoriteRepo, never()).save(any());
    }

    @Test
    @DisplayName("[TDD] 관심 공고 해제 시 freelancer 기준 삭제 메서드가 호출된다")
    void removeFavoriteJobPosting_callsDelete() {
        // given
        Long userId = 13L;
        Long jobPostingId = 104L;
        when(recruitmentUserReader.getFreelancerByIdOrThrow(userId))
                .thenReturn(new RecruitmentUser(123L, "freelancer", null, null, "ACTIVE"));

        // when
        jobPostingService.removeFavoriteJobPosting(userId, jobPostingId);

        // then
        verify(jobPostingFavoriteRepo, times(1)).deleteByFreelancerIdAndJobPostingId(123L, jobPostingId);
    }

    @Test
    @DisplayName("[TDD] favoritesOnly=true 검색 시 관심 공고만 반환되고 favorite 플래그가 true다")
    void searchJobPostingsForFreelancer_favoritesOnly_filtersAndMarksFavorite() {
        // given
        Long userId = 14L;
        Long freelancerId = 200L;
        when(recruitmentUserReader.getFreelancerByIdOrThrow(userId))
                .thenReturn(new RecruitmentUser(freelancerId, "freelancer", null, null, "ACTIVE"));

        JobPosting favoritePosting = createJobPosting(1L, "Spring Backend", Status.ACTIVE, JobPostingStatus.OPEN);
        JobPosting nonFavoritePosting = createJobPosting(2L, "React Frontend", Status.ACTIVE, JobPostingStatus.IN_PROGRESS);

        when(jobPostingFavoriteRepo.findAllByFreelancerId(freelancerId))
                .thenReturn(List.of(JobPostingFavorite.of(freelancerId, 1L)));
        when(jobPostingRepo.findAllByStatusAndPostingStatusIn(
                org.mockito.ArgumentMatchers.eq(Status.ACTIVE),
                org.mockito.ArgumentMatchers.<Set<JobPostingStatus>>any()
        )).thenReturn(List.of(favoritePosting, nonFavoritePosting));

        // when
        List<FreelancerJobPostingSearchDTO> result =
                jobPostingService.searchJobPostingsForFreelancer(userId, null, true);

        // then
        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).jobPostingId());
        assertEquals(true, result.get(0).favorite());
    }

    private JobPosting createJobPosting(Long id, String title, Status status, JobPostingStatus postingStatus) {
        JobPosting posting = JobPosting.from(
                new JobPostingCreateDTO(
                        title,
                        "desc",
                        List.of("Java"),
                        1000L,
                        3,
                        1
                ),
                1L,
                "employer"
        );
        ReflectionTestUtils.setField(posting, "id", id, Long.class);
        ReflectionTestUtils.setField(posting, "status", status);
        ReflectionTestUtils.setField(posting, "postingStatus", postingStatus);
        return posting;
    }
}
