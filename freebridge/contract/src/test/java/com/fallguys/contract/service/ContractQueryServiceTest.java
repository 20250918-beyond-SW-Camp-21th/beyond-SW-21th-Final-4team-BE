package com.fallguys.contract.service;

import com.fallguys.common.api.contract.ContractInfo;
import com.fallguys.contract.entity.Contract;
import com.fallguys.contract.entity.ContractStatus;
import com.fallguys.contract.repository.ContractRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ContractQueryServiceTest {

    @Mock
    private ContractRepository contractRepository;

    @InjectMocks
    private ContractQueryService contractQueryService;

    private Contract contract;

    @BeforeEach
    void setUp() {
        contract = new Contract();
        contract.setId(1L);
        contract.setContractId(1001L);
        contract.setProjectName("테스트 프로젝트");
        contract.setFreelancerId(100L);
        contract.setEmployerId(200L);
        contract.setCommissionRate(0.05);
        contract.setPaymentDay(25);
        contract.setStartDate(LocalDate.of(2024, 1, 1));
        contract.setEndDate(LocalDate.of(2024, 12, 31));
        contract.setBudget(5000000L);
        contract.setStatus(ContractStatus.IN_PROGRESS);
        contract.setEmployerSignature("employer-sign");
        contract.setFreelancerSignature("freelancer-sign");
    }

    @Test
    @DisplayName("existsContract()는 비즈니스 계약번호로만 계약 존재 여부를 확인한다")
    void existsContract_checksBusinessContractIdOnly() {
        when(contractRepository.existsByContractId(1001L))
                .thenReturn(true);

        boolean result = contractQueryService.existsContract(1001L);

        assertTrue(result);
        verify(contractRepository).existsByContractId(1001L);
        verify(contractRepository, never()).existsById(anyLong());
    }

    @Test
    @DisplayName("getContractInfoByContractId()는 비즈니스 계약번호로 계약 정보를 조회하여 반환한다")
    void getContractInfoByContractId_returnsContractInfo() {
        when(contractRepository.findByContractId(1001L))
                .thenReturn(Optional.of(contract));

        ContractInfo result = contractQueryService.getContractInfoByContractId(1001L);

        assertNotNull(result);
        assertEquals(1L, result.id());
        assertEquals(1001L, result.contractId());
        assertEquals("테스트 프로젝트", result.projectName());
        assertEquals(100L, result.freelancerId());
        assertEquals(200L, result.employerId());
        assertEquals("IN_PROGRESS", result.status());
        assertTrue(result.employerSigned());
        assertTrue(result.freelancerSigned());
        assertEquals(0.05, result.commissionRate());
        assertEquals(25, result.paymentDay());
        assertEquals(LocalDate.of(2024, 1, 1), result.startDate());
        assertEquals(LocalDate.of(2024, 12, 31), result.endDate());
        assertEquals(5000000L, result.budget());
        verify(contractRepository).findByContractId(1001L);
        verify(contractRepository, never()).findById(anyLong());
    }

    @Test
    @DisplayName("getContractInfoById()는 내부 PK로 계약 정보를 조회하여 반환한다")
    void getContractInfoById_returnsContractInfo() {
        when(contractRepository.findById(1L))
                .thenReturn(Optional.of(contract));

        ContractInfo result = contractQueryService.getContractInfoById(1L);

        assertNotNull(result);
        assertEquals(1L, result.id());
        assertEquals(1001L, result.contractId());
        verify(contractRepository).findById(1L);
        verify(contractRepository, never()).findByContractId(anyLong());
    }

    @Test
    @DisplayName("조회 타입을 명시하면 내부 PK와 비즈니스 계약번호가 충돌해도 올바른 계약을 반환한다")
    void explicitLookupApis_avoidCollisionBetweenInternalPkAndBusinessContractId() {
        Contract pkContract = new Contract();
        pkContract.setId(1001L);
        pkContract.setContractId(2001L);
        pkContract.setProjectName("PK 우선 계약");

        Contract businessContract = new Contract();
        businessContract.setId(2L);
        businessContract.setContractId(1001L);
        businessContract.setProjectName("비즈니스 계약번호 계약");

        when(contractRepository.findById(1001L)).thenReturn(Optional.of(pkContract));
        when(contractRepository.findByContractId(1001L)).thenReturn(Optional.of(businessContract));

        ContractInfo idLookup = contractQueryService.getContractInfoById(1001L);
        ContractInfo contractIdLookup = contractQueryService.getContractInfoByContractId(1001L);

        assertNotNull(idLookup);
        assertEquals(1001L, idLookup.id());
        assertEquals(2001L, idLookup.contractId());
        assertNotNull(contractIdLookup);
        assertEquals(2L, contractIdLookup.id());
        assertEquals(1001L, contractIdLookup.contractId());
        verify(contractRepository).findById(1001L);
        verify(contractRepository).findByContractId(1001L);
    }

    @Test
    @DisplayName("getContractInfoByContractId()는 존재하지 않는 비즈니스 계약번호 조회 시 예외를 발생시킨다")
    void getContractInfoByContractId_throwsExceptionWhenContractNotFound() {
        when(contractRepository.findByContractId(999L))
                .thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> contractQueryService.getContractInfoByContractId(999L)
        );

        assertTrue(exception.getMessage().contains("계약을 찾을 수 없습니다."));
    }

    @Test
    @DisplayName("getContractInfoById()는 존재하지 않는 내부 PK 조회 시 예외를 발생시킨다")
    void getContractInfoById_throwsExceptionWhenContractNotFound() {
        when(contractRepository.findById(999L))
                .thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> contractQueryService.getContractInfoById(999L)
        );

        assertTrue(exception.getMessage().contains("계약을 찾을 수 없습니다."));
        verify(contractRepository).findById(999L);
        verify(contractRepository, never()).findByContractId(anyLong());
    }

    @Test
    @DisplayName("getContractInfoByContractId()는 모든 필드가 null인 경우에도 비즈니스 계약번호로 동작한다")
    void getContractInfoByContractId_handlesNullFields() {
        Contract emptyContract = new Contract();
        emptyContract.setId(2L);
        emptyContract.setContractId(1002L);

        when(contractRepository.findByContractId(1002L))
                .thenReturn(Optional.of(emptyContract));

        ContractInfo result = contractQueryService.getContractInfoByContractId(1002L);

        assertNotNull(result);
        assertEquals(2L, result.id());
        assertEquals(1002L, result.contractId());
        assertNull(result.projectName());
        assertNull(result.status());
        assertFalse(result.employerSigned());
        assertFalse(result.freelancerSigned());
        verify(contractRepository).findByContractId(1002L);
        verify(contractRepository, never()).findById(anyLong());
    }
}
