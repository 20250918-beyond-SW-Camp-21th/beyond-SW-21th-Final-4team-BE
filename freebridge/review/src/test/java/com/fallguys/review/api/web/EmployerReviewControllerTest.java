package com.fallguys.review.api.web;

import com.fallguys.common.response.ApiResponse;
import com.fallguys.review.api.dto.request.EmployerReviewCreateRequest;
import com.fallguys.review.api.support.ReviewTokenUserIdResolver;
import com.fallguys.review.entity.FreelancerReview;
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
class EmployerReviewControllerTest {

    @Mock
    private ReviewService reviewService;

    @Mock
    private ReviewTokenUserIdResolver reviewTokenUserIdResolver;

    @InjectMocks
    private EmployerReviewController controller;

    @Test
    @DisplayName("[TDD] 고용주 리뷰 생성 시 pathVariable projectId를 요청 DTO에 반영해 서비스에 전달한다")
    void createEmployerReview_mergesProjectIdAndCallsService() {
        // given
        String authorization = "Bearer token";
        Long projectId = 777L;
        Long employerId = 11L;
        EmployerReviewCreateRequest request = new EmployerReviewCreateRequest(
                1L, 22L, 5, 5, 5, 5, 5, 5, "desc"
        );
        when(reviewTokenUserIdResolver.resolveUserId(authorization)).thenReturn(employerId);
        when(reviewService.createEmployerReview(org.mockito.ArgumentMatchers.eq(employerId), org.mockito.ArgumentMatchers.any()))
                .thenReturn(1000L);

        // when
        ResponseEntity<ApiResponse<Long>> response = controller.createEmployerReview(authorization, projectId, request);

        // then
        ArgumentCaptor<EmployerReviewCreateRequest> captor = ArgumentCaptor.forClass(EmployerReviewCreateRequest.class);
        verify(reviewService, times(1)).createEmployerReview(org.mockito.ArgumentMatchers.eq(employerId), captor.capture());
        assertEquals(projectId, captor.getValue().projectId());
        assertEquals(22L, captor.getValue().freelancerId());
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1000L, response.getBody().data());
    }

    @Test
    @DisplayName("[TDD] 고용주 수신 리뷰 조회 시 page/size를 안전값으로 보정해 서비스에 전달한다")
    void getEmployerReceivedReviews_clampsPageAndSize() {
        // given
        String authorization = "Bearer token";
        when(reviewTokenUserIdResolver.resolveUserId(authorization)).thenReturn(1L);
        Page<FreelancerReview> page = new PageImpl<>(List.of());
        when(reviewService.getEmployerReceivedReviews(org.mockito.ArgumentMatchers.eq(1L), org.mockito.ArgumentMatchers.any()))
                .thenReturn(page);

        // when
        controller.getEmployerReceivedReviews(authorization, -5, 500);

        // then
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(reviewService, times(1)).getEmployerReceivedReviews(org.mockito.ArgumentMatchers.eq(1L), pageableCaptor.capture());
        assertEquals(0, pageableCaptor.getValue().getPageNumber());
        assertEquals(100, pageableCaptor.getValue().getPageSize());
    }
}
