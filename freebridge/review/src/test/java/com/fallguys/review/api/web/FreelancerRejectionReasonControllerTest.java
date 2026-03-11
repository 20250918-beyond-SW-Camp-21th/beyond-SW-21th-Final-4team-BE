package com.fallguys.review.api.web;

import com.fallguys.common.response.ApiResponse;
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
class FreelancerRejectionReasonControllerTest {

    @Mock
    private EmployerRejectionReasonService employerRejectionReasonService;

    @Mock
    private ReviewTokenUserIdResolver reviewTokenUserIdResolver;

    @InjectMocks
    private FreelancerRejectionReasonController controller;

    @Test
    @DisplayName("[TDD] 프리랜서 거절 사유 조회 시 page/size를 안전값으로 보정해 서비스에 전달한다")
    void getFreelancerRejectionReasons_clampsPageAndSize() {
        // given
        String authorization = "Bearer token";
        when(reviewTokenUserIdResolver.resolveUserId(authorization)).thenReturn(31L);
        Page<EmployerRejectionReasonResponseDTO> page = new PageImpl<>(List.of(
                new EmployerRejectionReasonResponseDTO(1L, 10L, "플랫폼 구축", 1L, 31L, "사유", LocalDateTime.now())
        ));
        when(employerRejectionReasonService.getFreelancerRejectionReasons(
                org.mockito.ArgumentMatchers.eq(31L),
                org.mockito.ArgumentMatchers.eq("플랫폼"),
                org.mockito.ArgumentMatchers.any()
        )).thenReturn(page);

        // when
        ResponseEntity<ApiResponse<com.fallguys.review.api.dto.response.PagedResponseDTO<EmployerRejectionReasonResponseDTO>>> response =
                controller.getFreelancerRejectionReasons(authorization, "플랫폼", -3, 500);

        // then
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(employerRejectionReasonService, times(1))
                .getFreelancerRejectionReasons(org.mockito.ArgumentMatchers.eq(31L), org.mockito.ArgumentMatchers.eq("플랫폼"), pageableCaptor.capture());
        assertEquals(0, pageableCaptor.getValue().getPageNumber());
        assertEquals(100, pageableCaptor.getValue().getPageSize());
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().data().totalElements());
    }
}
