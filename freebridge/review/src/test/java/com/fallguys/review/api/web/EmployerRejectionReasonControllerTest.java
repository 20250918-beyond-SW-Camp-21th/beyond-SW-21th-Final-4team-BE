package com.fallguys.review.api.web;

import com.fallguys.common.response.ApiResponse;
import com.fallguys.review.api.dto.request.EmployerRejectionReasonCreateRequest;
import com.fallguys.review.api.dto.response.EmployerRejectionReasonResponseDTO;
import com.fallguys.review.api.support.ReviewTokenUserIdResolver;
import com.fallguys.review.service.EmployerRejectionReasonService;
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

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmployerRejectionReasonControllerTest {

    @Mock
    private EmployerRejectionReasonService employerRejectionReasonService;

    @Mock
    private ReviewTokenUserIdResolver reviewTokenUserIdResolver;

    @InjectMocks
    private EmployerRejectionReasonController controller;

    @Test
    @DisplayName("[TDD] 고용주 거절 사유 생성 시 토큰의 사용자 ID로 저장 요청을 전달한다")
    void createEmployerRejectionReason_callsService() {
        // given
        String authorization = "Bearer token";
        Long employerId = 11L;
        EmployerRejectionReasonCreateRequest request = new EmployerRejectionReasonCreateRequest(
                7L,
                "플랫폼 구축",
                22L,
                "기술 스택이 맞지 않음"
        );
        when(reviewTokenUserIdResolver.resolveUserId(authorization)).thenReturn(employerId);
        when(employerRejectionReasonService.createEmployerRejectionReason(employerId, request)).thenReturn(101L);

        // when
        ResponseEntity<ApiResponse<Long>> response = controller.createEmployerRejectionReason(authorization, request);

        // then
        verify(employerRejectionReasonService, times(1)).createEmployerRejectionReason(employerId, request);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(101L, response.getBody().data());
    }

    @Test
    @DisplayName("[TDD] 고용주 거절 사유 조회 시 page/size를 안전값으로 보정하고 제목 검색어를 전달한다")
    void getEmployerRejectionReasons_clampsPageAndSize() {
        // given
        String authorization = "Bearer token";
        when(reviewTokenUserIdResolver.resolveUserId(authorization)).thenReturn(1L);
        Page<EmployerRejectionReasonResponseDTO> page = new PageImpl<>(List.of(
                new EmployerRejectionReasonResponseDTO(1L, 10L, "결제 모듈", 1L, 2L, "사유", LocalDateTime.now())
        ));
        when(employerRejectionReasonService.getEmployerRejectionReasons(org.mockito.ArgumentMatchers.eq(1L), org.mockito.ArgumentMatchers.eq("결제"), org.mockito.ArgumentMatchers.any()))
                .thenReturn(page);

        // when
        controller.getEmployerRejectionReasons(authorization, "결제", -1, 1000);

        // then
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(employerRejectionReasonService, times(1))
                .getEmployerRejectionReasons(org.mockito.ArgumentMatchers.eq(1L), org.mockito.ArgumentMatchers.eq("결제"), pageableCaptor.capture());
        assertEquals(0, pageableCaptor.getValue().getPageNumber());
        assertEquals(100, pageableCaptor.getValue().getPageSize());
    }
}
