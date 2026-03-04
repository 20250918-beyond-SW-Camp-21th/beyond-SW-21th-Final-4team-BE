package com.fallguys.matchs.api.web;

import com.fallguys.common.response.ApiResponse;
import com.fallguys.matchs.api.dto.request.ApplicationCreateRequest;
import com.fallguys.matchs.api.dto.response.ProposalResponseDTO;
import com.fallguys.matchs.api.support.TokenUserIdResolver;
import com.fallguys.matchs.entity.MatchsStatus;
import com.fallguys.matchs.service.MatchsService;
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
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MatchsControllerTest {

    @Mock
    private MatchsService matchsService;

    @Mock
    private TokenUserIdResolver tokenUserIdResolver;

    @InjectMocks
    private MatchsController controller;

    @Test
    @DisplayName("[TDD] 지원 생성 API는 토큰 userId를 해석해 서비스 호출 후 applicationId를 반환")
    void createApplication_callsServiceAndReturnsId() {
        // given
        String authorization = "Bearer token";
        ApplicationCreateRequest request = new ApplicationCreateRequest(10L, "message");
        when(tokenUserIdResolver.resolveUserId(authorization)).thenReturn(3L);
        when(matchsService.createApplication(3L, request)).thenReturn(77L);

        // when
        ResponseEntity<ApiResponse<Map<String, Long>>> response = controller.createApplication(authorization, request);

        // then
        verify(matchsService, times(1)).createApplication(3L, request);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(77L, response.getBody().data().get("applicationId"));
    }

    @Test
    @DisplayName("[TDD] 고용주 제안 목록 조회 API는 page/size를 안전값으로 보정한다")
    void getEmployerProposals_clampsPageAndSize() {
        // given
        String authorization = "Bearer token";
        when(tokenUserIdResolver.resolveUserId(authorization)).thenReturn(5L);
        Page<ProposalResponseDTO> page = new PageImpl<>(List.of(
                new ProposalResponseDTO(1L, 2L, 3L, 5L, "msg", MatchsStatus.PENDING, LocalDateTime.now())
        ));
        when(matchsService.getEmployerProposals(org.mockito.ArgumentMatchers.eq(5L), org.mockito.ArgumentMatchers.any()))
                .thenReturn(page);

        // when
        ResponseEntity<ApiResponse<com.fallguys.matchs.api.dto.response.PagedResponseDTO<ProposalResponseDTO>>> response =
                controller.getEmployerProposals(authorization, -1, 999);

        // then
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(matchsService, times(1)).getEmployerProposals(org.mockito.ArgumentMatchers.eq(5L), pageableCaptor.capture());
        assertEquals(0, pageableCaptor.getValue().getPageNumber());
        assertEquals(100, pageableCaptor.getValue().getPageSize());
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }
}
