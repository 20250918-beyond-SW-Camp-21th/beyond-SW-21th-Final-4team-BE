package com.fallguys.review.api.web;

import com.fallguys.common.response.ApiResponse;
import com.fallguys.review.api.dto.request.FreelancerReviewCreateRequest;
import com.fallguys.review.api.support.ReviewTokenUserIdResolver;
import com.fallguys.review.entity.EmployerReview;
import com.fallguys.review.service.ReviewService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FreelancerReviewControllerTest {

    @Mock
    private ReviewService reviewService;

    @Mock
    private ReviewTokenUserIdResolver reviewTokenUserIdResolver;

    @InjectMocks
    private FreelancerReviewController controller;

    @Test
    @DisplayName("[TDD] 프리랜서 리뷰 생성 시 pathVariable projectId를 요청 DTO에 반영해 서비스에 전달한다")
    void createFreelancerReview_mergesProjectIdAndCallsService() {
        // given
        String authorization = "Bearer token";
        Long projectId = 501L;
        Long freelancerId = 31L;
        FreelancerReviewCreateRequest request = new FreelancerReviewCreateRequest(
                1L, 41L, 5, 4, 3, "desc"
        );
        when(reviewTokenUserIdResolver.resolveUserId(authorization)).thenReturn(freelancerId);
        when(reviewService.createFreelancerReview(org.mockito.ArgumentMatchers.eq(freelancerId), org.mockito.ArgumentMatchers.any()))
                .thenReturn(888L);

        // when
        ResponseEntity<ApiResponse<Long>> response = controller.createFreelancerReview(authorization, projectId, request);

        // then
        ArgumentCaptor<FreelancerReviewCreateRequest> captor = ArgumentCaptor.forClass(FreelancerReviewCreateRequest.class);
        verify(reviewService, times(1)).createFreelancerReview(org.mockito.ArgumentMatchers.eq(freelancerId), captor.capture());
        assertEquals(projectId, captor.getValue().projectId());
        assertEquals(41L, captor.getValue().employerId());
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(888L, response.getBody().data());
    }

    @Test
    @DisplayName("[TDD] 프리랜서 수신 리뷰 조회 시 page/size를 안전값으로 보정해 서비스에 전달한다")
    void getFreelancerReceivedReviews_clampsPageAndSize() {
        // given
        String authorization = "Bearer token";
        when(reviewTokenUserIdResolver.resolveUserId(authorization)).thenReturn(31L);
        Page<EmployerReview> page = new PageImpl<>(List.of());
        when(reviewService.getFreelancerReceivedReviews(org.mockito.ArgumentMatchers.eq(31L), org.mockito.ArgumentMatchers.any()))
                .thenReturn(page);

        // when
        controller.getFreelancerReceivedReviews(authorization, -1, 1000);

        // then
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(reviewService, times(1)).getFreelancerReceivedReviews(org.mockito.ArgumentMatchers.eq(31L), pageableCaptor.capture());
        assertEquals(0, pageableCaptor.getValue().getPageNumber());
        assertEquals(100, pageableCaptor.getValue().getPageSize());
    }
}
