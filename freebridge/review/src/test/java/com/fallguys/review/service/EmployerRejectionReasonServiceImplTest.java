package com.fallguys.review.service;

import com.fallguys.review.api.dto.request.EmployerRejectionReasonCreateRequest;
import com.fallguys.review.api.dto.response.EmployerRejectionReasonResponseDTO;
import com.fallguys.review.entity.EmployerRejectionReason;
import com.fallguys.review.repository.EmployerRejectionReasonRepository;
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
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmployerRejectionReasonServiceImplTest {

    @Mock
    private EmployerRejectionReasonRepository employerRejectionReasonRepository;

    @InjectMocks
    private EmployerRejectionReasonServiceImpl employerRejectionReasonService;

    @Test
    @DisplayName("[TDD] 고용주 거절 사유 생성 시 employerId를 포함해 저장한다")
    void createEmployerRejectionReason_savesReason() {
        // given
        Long employerId = 1L;
        EmployerRejectionReasonCreateRequest request = new EmployerRejectionReasonCreateRequest(
                10L,
                "  프리브릿지 플랫폼 구축  ",
                20L,
                "  포트폴리오와 요구사항이 맞지 않았습니다.  "
        );
        when(employerRejectionReasonRepository.save(any())).thenAnswer(invocation -> {
            EmployerRejectionReason rejectionReason = invocation.getArgument(0);
            ReflectionTestUtils.setField(rejectionReason, "id", 100L, Long.class);
            return rejectionReason;
        });

        // when
        Long savedId = employerRejectionReasonService.createEmployerRejectionReason(employerId, request);

        // then
        assertEquals(100L, savedId);
        ArgumentCaptor<EmployerRejectionReason> captor = ArgumentCaptor.forClass(EmployerRejectionReason.class);
        verify(employerRejectionReasonRepository, times(1)).save(captor.capture());
        assertEquals(employerId, captor.getValue().getEmployerId());
        assertEquals("프리브릿지 플랫폼 구축", captor.getValue().getProjectTitle());
        assertEquals("포트폴리오와 요구사항이 맞지 않았습니다.", captor.getValue().getReason());
    }

    @Test
    @DisplayName("[TDD] 프로젝트 제목 검색어가 있으면 제목 필터 조회를 수행한다")
    void getEmployerRejectionReasons_withTitle_usesFilteredQuery() {
        // given
        Long employerId = 2L;
        PageRequest pageable = PageRequest.of(0, 10);
        EmployerRejectionReason rejectionReason = EmployerRejectionReason.builder()
                .id(1L)
                .projectId(10L)
                .projectTitle("결제 모듈 테스트")
                .employerId(employerId)
                .freelancerId(30L)
                .reason("요구 기술과 맞지 않음")
                .build();
        ReflectionTestUtils.setField(rejectionReason, "createdAt", java.time.LocalDateTime.of(2026, 3, 11, 10, 0));
        when(employerRejectionReasonRepository.findAllByEmployerIdAndProjectTitleContainingIgnoreCaseOrderByCreatedAtDesc(
                employerId,
                "결제",
                pageable
        )).thenReturn(new PageImpl<>(List.of(rejectionReason), pageable, 1));

        // when
        Page<EmployerRejectionReasonResponseDTO> result = employerRejectionReasonService.getEmployerRejectionReasons(
                employerId,
                "  결제  ",
                pageable
        );

        // then
        assertEquals(1, result.getTotalElements());
        assertEquals("결제 모듈 테스트", result.getContent().get(0).projectTitle());
        verify(employerRejectionReasonRepository, times(1))
                .findAllByEmployerIdAndProjectTitleContainingIgnoreCaseOrderByCreatedAtDesc(employerId, "결제", pageable);
    }

    @Test
    @DisplayName("[TDD] 프리랜서 거절 사유 조회 시 freelancerId 기준 목록을 반환한다")
    void getFreelancerRejectionReasons_usesFreelancerQuery() {
        // given
        Long freelancerId = 30L;
        PageRequest pageable = PageRequest.of(0, 10);
        EmployerRejectionReason rejectionReason = EmployerRejectionReason.builder()
                .id(2L)
                .projectId(20L)
                .projectTitle("플랫폼 구축")
                .employerId(4L)
                .freelancerId(freelancerId)
                .reason("현재 요구 경력과 차이가 있습니다.")
                .build();
        ReflectionTestUtils.setField(rejectionReason, "createdAt", java.time.LocalDateTime.of(2026, 3, 11, 12, 0));
        when(employerRejectionReasonRepository.findAllByFreelancerIdOrderByCreatedAtDesc(
                freelancerId,
                pageable
        )).thenReturn(new PageImpl<>(List.of(rejectionReason), pageable, 1));

        // when
        Page<EmployerRejectionReasonResponseDTO> result = employerRejectionReasonService.getFreelancerRejectionReasons(
                freelancerId,
                null,
                pageable
        );

        // then
        assertEquals(1, result.getTotalElements());
        assertEquals("플랫폼 구축", result.getContent().get(0).projectTitle());
        assertEquals("현재 요구 경력과 차이가 있습니다.", result.getContent().get(0).reason());
        verify(employerRejectionReasonRepository, times(1))
                .findAllByFreelancerIdOrderByCreatedAtDesc(freelancerId, pageable);
    }
}
