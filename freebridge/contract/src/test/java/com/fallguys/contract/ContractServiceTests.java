package com.fallguys.contract;

import com.fallguys.common.exception.BusinessException;
import com.fallguys.common.exception.ErrorCode;
import com.fallguys.contract.repository.ContractRepository;
import com.fallguys.contract.service.ContractPdfService;
import com.fallguys.contract.service.ContractService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ContractServiceTests {

    @Mock
    private ContractRepository contractRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private ContractPdfService contractPdfService;

    @InjectMocks
    private ContractService contractService;

    // ── listContracts ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("listContracts() — role 검증")
    class ListContracts {

        @Test
        @DisplayName("role이 null이면 INVALID_INPUT_VALUE 에러가 발생한다")
        void nullRole_throws() {
            BusinessException ex = assertThrows(BusinessException.class,
                    () -> contractService.listContracts(1L, null, null, null, 1, 10));

            assertEquals(ErrorCode.INVALID_INPUT_VALUE, ex.getErrorCode());
        }

        @Test
        @DisplayName("유효하지 않은 role 값이면 INVALID_INPUT_VALUE 에러가 발생한다")
        void invalidRole_throws() {
            BusinessException ex = assertThrows(BusinessException.class,
                    () -> contractService.listContracts(1L, "ADMIN", null, null, 1, 10));

            assertEquals(ErrorCode.INVALID_INPUT_VALUE, ex.getErrorCode());
        }

        @Test
        @DisplayName("빈 문자열 role이면 INVALID_INPUT_VALUE 에러가 발생한다")
        void emptyRole_throws() {
            BusinessException ex = assertThrows(BusinessException.class,
                    () -> contractService.listContracts(1L, "", null, null, 1, 10));

            assertEquals(ErrorCode.INVALID_INPUT_VALUE, ex.getErrorCode());
        }

        @Test
        @DisplayName("role이 EMPLOYER이면 고용주 계약 목록을 조회한다")
        void employerRole_queriesByEmployerId() {
            when(contractRepository.findByEmployerIdOrderByIdDesc(1L))
                    .thenReturn(Collections.emptyList());

            contractService.listContracts(1L, "EMPLOYER", null, null, 1, 10);

            org.mockito.Mockito.verify(contractRepository).findByEmployerIdOrderByIdDesc(1L);
        }

        @Test
        @DisplayName("role이 FREELANCER이면 프리랜서 계약 목록을 조회한다")
        void freelancerRole_queriesByFreelancerId() {
            when(contractRepository.findByFreelancerIdOrderByIdDesc(2L))
                    .thenReturn(Collections.emptyList());

            contractService.listContracts(2L, "FREELANCER", null, null, 1, 10);

            org.mockito.Mockito.verify(contractRepository).findByFreelancerIdOrderByIdDesc(2L);
        }
    }
}