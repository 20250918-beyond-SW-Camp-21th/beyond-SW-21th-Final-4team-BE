package com.fallguys.review.service;

import com.fallguys.common.exception.BusinessException;
import com.fallguys.common.exception.ErrorCode;
import com.fallguys.common.port.ProjectExternalApi;
import com.fallguys.review.api.dto.request.EmployerReviewCreateRequest;
import com.fallguys.review.api.dto.request.EmployerReviewUpdateRequest;
import com.fallguys.review.api.dto.request.FreelancerReviewCreateRequest;
import com.fallguys.review.api.dto.request.FreelancerReviewUpdateRequest;
import com.fallguys.review.entity.EmployerReview;
import com.fallguys.review.entity.FreelancerReview;
import com.fallguys.review.entity.ReviewStatus;
import com.fallguys.review.repository.EmployerReviewRepository;
import com.fallguys.review.repository.FreelancerReviewRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.sql.SQLException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReviewServiceImplTest {

    @Mock
    private EmployerReviewRepository employerReviewRepository;

    @Mock
    private FreelancerReviewRepository freelancerReviewRepository;

    @Mock
    private ProjectExternalApi projectExternalApi;

    @InjectMocks
    private ReviewServiceImpl reviewService;

    @Test
    @DisplayName("[TDD] 고용주 리뷰 생성 시 ACTIVE 중복 데이터가 있으면 INVALID_INPUT_VALUE 예외")
    void createEmployerReview_whenAlreadyExists_throwsBusinessException() {
        // given
        Long employerId = 1L;
        EmployerReviewCreateRequest request = new EmployerReviewCreateRequest(10L, 20L, 5, 5, 5, 5, 5, 5, "desc");
        when(employerReviewRepository.findByProjectIdAndEmployerIdAndFreelancerIdAndStatus(
                request.projectId(), employerId, request.freelancerId(), ReviewStatus.ACTIVE
        )).thenReturn(Optional.of(EmployerReview.builder().id(99L).build()));

        // when
        BusinessException ex = assertThrows(BusinessException.class, () -> reviewService.createEmployerReview(employerId, request));

        // then
        assertEquals(ErrorCode.INVALID_INPUT_VALUE, ex.getErrorCode());
    }

    @Test
    @DisplayName("[TDD] 고용주 리뷰 생성 성공 시 저장된 리뷰 ID를 반환한다")
    void createEmployerReview_success_returnsSavedId() {
        // given
        Long employerId = 1L;
        EmployerReviewCreateRequest request = new EmployerReviewCreateRequest(10L, 20L, 4, 4, 4, 4, 4, 4, "good");
        when(employerReviewRepository.findByProjectIdAndEmployerIdAndFreelancerIdAndStatus(
                request.projectId(), employerId, request.freelancerId(), ReviewStatus.ACTIVE
        )).thenReturn(Optional.empty());
        when(employerReviewRepository.save(any())).thenReturn(EmployerReview.builder().id(123L).build());

        // when
        Long reviewId = reviewService.createEmployerReview(employerId, request);

        // then
        assertEquals(123L, reviewId);
        ArgumentCaptor<EmployerReview> captor = ArgumentCaptor.forClass(EmployerReview.class);
        verify(employerReviewRepository, times(1)).save(captor.capture());
        assertEquals(employerId, captor.getValue().getEmployerId());
        assertEquals(request.freelancerId(), captor.getValue().getFreelancerId());
        verify(projectExternalApi, times(1)).completeProjectWithReview(any());
    }

    @Test
    @DisplayName("[TDD] 고용주 리뷰 생성 시 중복키 예외(SQLState 23505)는 BusinessException으로 변환")
    void createEmployerReview_duplicateSqlState_throwsBusinessException() {
        // given
        Long employerId = 1L;
        EmployerReviewCreateRequest request = new EmployerReviewCreateRequest(10L, 20L, 3, 3, 3, 3, 3, 3, "dup");
        when(employerReviewRepository.findByProjectIdAndEmployerIdAndFreelancerIdAndStatus(
                request.projectId(), employerId, request.freelancerId(), ReviewStatus.ACTIVE
        )).thenReturn(Optional.empty());
        DataIntegrityViolationException ex = new DataIntegrityViolationException(
                "duplicate", new SQLException("duplicate key", "23505")
        );
        when(employerReviewRepository.save(any())).thenThrow(ex);

        // when
        BusinessException thrown = assertThrows(BusinessException.class, () -> reviewService.createEmployerReview(employerId, request));

        // then
        assertEquals(ErrorCode.INVALID_INPUT_VALUE, thrown.getErrorCode());
    }

    @Test
    @DisplayName("[TDD] 고용주 리뷰 수정 시 대상 리뷰를 찾아 값이 갱신된다")
    void updateEmployerReview_updatesFields() {
        // given
        Long employerId = 2L;
        Long reviewId = 3L;
        EmployerReview review = EmployerReview.builder()
                .id(reviewId).employerId(employerId).freelancerId(30L)
                .language(1).framework(1).debugging(1).communication(1).schedule(1).dispute(1)
                .description("old").status(ReviewStatus.ACTIVE).build();
        EmployerReviewUpdateRequest request = new EmployerReviewUpdateRequest(5, 4, 3, 2, 1, 5, "new");
        when(employerReviewRepository.findByIdAndEmployerIdAndStatus(reviewId, employerId, ReviewStatus.ACTIVE))
                .thenReturn(Optional.of(review));

        // when
        reviewService.updateEmployerReview(employerId, reviewId, request);

        // then
        assertEquals(5, review.getLanguage());
        assertEquals(4, review.getFramework());
        assertEquals("new", review.getDescription());
    }

    @Test
    @DisplayName("[TDD] 고용주 리뷰 삭제 시 soft delete 처리된다")
    void deleteEmployerReview_softDeletes() {
        // given
        Long employerId = 2L;
        Long reviewId = 4L;
        EmployerReview review = EmployerReview.builder()
                .id(reviewId).employerId(employerId).status(ReviewStatus.ACTIVE).build();
        when(employerReviewRepository.findByIdAndEmployerIdAndStatus(reviewId, employerId, ReviewStatus.ACTIVE))
                .thenReturn(Optional.of(review));

        // when
        reviewService.deleteEmployerReview(employerId, reviewId);

        // then
        assertEquals(ReviewStatus.DELETED, review.getStatus());
        assertEquals(true, review.getDeleted());
    }

    @Test
    @DisplayName("[TDD] 프리랜서 리뷰 생성 성공 시 저장된 리뷰 ID를 반환한다")
    void createFreelancerReview_success_returnsSavedId() {
        // given
        Long freelancerId = 7L;
        FreelancerReviewCreateRequest request = new FreelancerReviewCreateRequest(100L, 200L, 5, 4, 3, "ok");
        when(freelancerReviewRepository.findByProjectIdAndFreelancerIdAndEmployerIdAndStatus(
                request.projectId(), freelancerId, request.employerId(), ReviewStatus.ACTIVE
        )).thenReturn(Optional.empty());
        when(freelancerReviewRepository.save(any())).thenReturn(FreelancerReview.builder().id(55L).build());

        // when
        Long reviewId = reviewService.createFreelancerReview(freelancerId, request);

        // then
        assertEquals(55L, reviewId);
        ArgumentCaptor<FreelancerReview> captor = ArgumentCaptor.forClass(FreelancerReview.class);
        verify(freelancerReviewRepository, times(1)).save(captor.capture());
        assertEquals(freelancerId, captor.getValue().getFreelancerId());
        assertEquals(request.employerId(), captor.getValue().getEmployerId());
    }

    @Test
    @DisplayName("[TDD] 프리랜서 리뷰 수정 시 대상 리뷰를 찾아 값이 갱신된다")
    void updateFreelancerReview_updatesFields() {
        // given
        Long freelancerId = 8L;
        Long reviewId = 9L;
        FreelancerReview review = FreelancerReview.builder()
                .id(reviewId).freelancerId(freelancerId).employerId(300L)
                .atmosphere(1).requirementDetail(1).schedule(1).description("old")
                .status(ReviewStatus.ACTIVE).build();
        FreelancerReviewUpdateRequest request = new FreelancerReviewUpdateRequest(2, 3, 4, "new");
        when(freelancerReviewRepository.findByIdAndFreelancerIdAndStatus(reviewId, freelancerId, ReviewStatus.ACTIVE))
                .thenReturn(Optional.of(review));

        // when
        reviewService.updateFreelancerReview(freelancerId, reviewId, request);

        // then
        assertEquals(2, review.getAtmosphere());
        assertEquals(3, review.getRequirementDetail());
        assertEquals("new", review.getDescription());
    }

    @Test
    @DisplayName("[TDD] 프리랜서 리뷰 삭제 시 soft delete 처리된다")
    void deleteFreelancerReview_softDeletes() {
        // given
        Long freelancerId = 8L;
        Long reviewId = 10L;
        FreelancerReview review = FreelancerReview.builder()
                .id(reviewId).freelancerId(freelancerId).status(ReviewStatus.ACTIVE).build();
        when(freelancerReviewRepository.findByIdAndFreelancerIdAndStatus(reviewId, freelancerId, ReviewStatus.ACTIVE))
                .thenReturn(Optional.of(review));

        // when
        reviewService.deleteFreelancerReview(freelancerId, reviewId);

        // then
        assertEquals(ReviewStatus.DELETED, review.getStatus());
        assertEquals(true, review.getDeleted());
    }
}
