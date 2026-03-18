package com.fallguys.contract.service;

import com.fallguys.contract.api.web.PaginationInfo;
import com.fallguys.common.exception.BusinessException;
import com.fallguys.common.exception.ErrorCode;
import com.fallguys.contract.api.shared.ContractActivatedEvent;
import com.fallguys.contract.api.web.dto.*;
import com.fallguys.contract.entity.Contract;
import com.fallguys.contract.entity.ContractStatus;
import com.fallguys.contract.repository.ContractRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ContractServiceTest {

    @Mock
    private ContractRepository contractRepository;

    @Mock
    private ContractPdfService contractPdfService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private ContractService contractService;

    private CreateContractRequest createRequest;
    private Contract savedContract;

    private SignContractRequest signRequest(String signature) {
        SignContractRequest request = new SignContractRequest();
        request.setSignature(signature);
        return request;
    }

    @BeforeEach
    void setUp() {
        createRequest = new CreateContractRequest();
        createRequest.setProjectName("테스트 프로젝트");
        createRequest.setFreelancerId(100L);
        createRequest.setRelatedJobId("job-123");
        createRequest.setRelatedApplicationId("app-456");
        createRequest.setRelatedProposalId("proposal-789");
        createRequest.setStartDate(LocalDate.of(2024, 1, 1));
        createRequest.setEndDate(LocalDate.of(2024, 12, 31));
        createRequest.setBudget(5000000L);
        createRequest.setPaymentDay(25);
        createRequest.setJobDescription("웹 개발");
        createRequest.setWorkLocation("원격근무");
        createRequest.setWorkStartTime("09:00");
        createRequest.setWorkEndTime("18:00");
        createRequest.setBreakStartTime("12:00");
        createRequest.setBreakEndTime("13:00");
        createRequest.setWorkDaysPerWeek(5);
        createRequest.setWeeklyHoliday("토, 일");
        createRequest.setEmployerBusinessName("테스트회사");
        createRequest.setEmployerAddress("서울시 강남구");
        createRequest.setEmployerCEO("김대표");
        createRequest.setFreelancerAddress("서울시 마포구");
        createRequest.setFreelancerPhone("010-1234-5678");
        createRequest.setEmployerSignature("data:image/png;base64,abc123");

        savedContract = new Contract();
        savedContract.setId(1L);
        savedContract.setContractId(1001L);
        savedContract.setProjectName("테스트 프로젝트");
        savedContract.setStatus(ContractStatus.WAITING_SIGNATURE);
        savedContract.setEmployerId(200L);
        savedContract.setFreelancerId(100L);
    }


    @Nested
    @DisplayName("createContract()")
    class CreateContract {

        @Test
        @DisplayName("계약 생성 시 모든 필드가 올바르게 설정된다")
        void createsContractWithAllFields() {
            when(contractRepository.save(any(Contract.class)))
                    .thenAnswer(invocation -> {
                        Contract contract = invocation.getArgument(0);
                        contract.setId(1L);
                        return contract;
                    });
            when(contractPdfService.generateContractPdf(any(Contract.class)))
                    .thenReturn("/pdfs/contracts/1001_contract.pdf");

            ContractResponse response = contractService.createContract(createRequest, 200L);

            assertNotNull(response);
            assertEquals("테스트 프로젝트", response.getProjectName());
            assertEquals(100L, response.getFreelancerId());
            assertEquals(200L, response.getEmployerId());
            assertEquals("job-123", response.getRelatedJobId());
            assertEquals("app-456", response.getRelatedApplicationId());
            assertEquals("proposal-789", response.getRelatedProposalId());
            assertEquals(5000000L, response.getBudget());
            assertEquals("WAITING_SIGNATURE", response.getStatus());
        }

        @Test
        @DisplayName("계약 생성 시 contractId는 id + 1000으로 설정된다")
        void setsContractIdAsIdPlus1000() {
            when(contractRepository.save(any(Contract.class)))
                    .thenAnswer(invocation -> {
                        Contract contract = invocation.getArgument(0);
                        contract.setId(5L);
                        return contract;
                    });
            when(contractPdfService.generateContractPdf(any(Contract.class)))
                    .thenReturn("/pdfs/contracts/1005_contract.pdf");

            contractService.createContract(createRequest, 200L);

            ArgumentCaptor<Contract> captor = ArgumentCaptor.forClass(Contract.class);
            verify(contractRepository, times(2)).save(captor.capture());

            Contract savedContract = captor.getAllValues().get(1);
            assertEquals(1005L, savedContract.getContractId());
        }

        @Test
        @DisplayName("계약 생성 시 PDF가 생성되고 URL이 저장된다")
        void generatesPdfAndStoresUrl() {
            String expectedPdfUrl = "/pdfs/contracts/1001_contract.pdf";

            when(contractRepository.save(any(Contract.class)))
                    .thenAnswer(invocation -> {
                        Contract contract = invocation.getArgument(0);
                        contract.setId(1L);
                        return contract;
                    });
            when(contractPdfService.generateContractPdf(any(Contract.class)))
                    .thenReturn(expectedPdfUrl);

            ContractResponse response = contractService.createContract(createRequest, 200L);

            verify(contractPdfService).generateContractPdf(any(Contract.class));
            assertEquals(expectedPdfUrl, response.getContractPdfUrl());
        }

        @Test
        @DisplayName("고용주 서명이 제공되면 서명이 설정된다")
        void setsEmployerSignatureIfProvided() {
            when(contractRepository.save(any(Contract.class)))
                    .thenAnswer(invocation -> {
                        Contract contract = invocation.getArgument(0);
                        contract.setId(1L);
                        return contract;
                    });
            when(contractPdfService.generateContractPdf(any(Contract.class)))
                    .thenReturn("/pdfs/contracts/1001_contract.pdf");

            ContractResponse response = contractService.createContract(createRequest, 200L);

            assertNotNull(response.getEmployerSignature());
            assertEquals("data:image/png;base64,abc123", response.getEmployerSignature());
            assertNotNull(response.getEmployerSignedDate());
            assertTrue(Boolean.TRUE.equals(response.getEmployerSigned()));
            assertFalse(Boolean.TRUE.equals(response.getFreelancerSigned()));
        }

        @Test
        @DisplayName("수수료율이 기본값 0.05로 설정된다")
        void setsDefaultCommissionRate() {
            when(contractRepository.save(any(Contract.class)))
                    .thenAnswer(invocation -> {
                        Contract contract = invocation.getArgument(0);
                        contract.setId(1L);
                        return contract;
                    });
            when(contractPdfService.generateContractPdf(any(Contract.class)))
                    .thenReturn("/pdfs/contracts/1001_contract.pdf");

            ContractResponse response = contractService.createContract(createRequest, 200L);

            assertEquals(0.05, response.getCommissionRate());
        }
    }

    @Nested
    @DisplayName("listContracts()")
    class ListContracts {

        private List<Contract> mockContracts;

        @BeforeEach
        void setUp() {
            Contract c1 = new Contract();
            c1.setId(1L);
            c1.setContractId(1001L);
            c1.setProjectName("프로젝트A");
            c1.setStatus(ContractStatus.WAITING_SIGNATURE);
            c1.setFreelancerId(100L);
            c1.setEmployerId(200L);
            c1.setBudget(5000000L);

            Contract c2 = new Contract();
            c2.setId(2L);
            c2.setContractId(1002L);
            c2.setProjectName("프로젝트B");
            c2.setStatus(ContractStatus.IN_PROGRESS);
            c2.setFreelancerId(100L);
            c2.setEmployerId(200L);
            c2.setBudget(8000000L);
            c2.setEmployerSignature("sig1");
            c2.setFreelancerSignature("sig2");

            mockContracts = Arrays.asList(c1, c2);
        }

        @Test
        @DisplayName("고용주 역할로 조회 시 employerId로 필터링된다")
        void filtersEmployerRole() {
            when(contractRepository.findByEmployerIdOrderByIdDesc(200L))
                    .thenReturn(mockContracts);

            ContractListResponse response = contractService.listContracts(
                    200L, "EMPLOYER", null, null, 1, 10);

            verify(contractRepository).findByEmployerIdOrderByIdDesc(200L);
            verify(contractRepository, never()).findByFreelancerIdOrderByIdDesc(any());
            assertEquals(2, response.getItems().size());
        }

        @Test
        @DisplayName("프리랜서 역할로 조회 시 freelancerId로 필터링된다")
        void filtersFreelancerRole() {
            when(contractRepository.findByFreelancerIdOrderByIdDesc(100L))
                    .thenReturn(mockContracts);

            ContractListResponse response = contractService.listContracts(
                    100L, "FREELANCER", null, null, 1, 10);

            verify(contractRepository).findByFreelancerIdOrderByIdDesc(100L);
            verify(contractRepository, never()).findByEmployerIdOrderByIdDesc(any());
            assertEquals(2, response.getItems().size());
        }

        @Test
        @DisplayName("상태로 필터링할 수 있다")
        void filtersByStatus() {
            when(contractRepository.findByEmployerIdOrderByIdDesc(200L))
                    .thenReturn(mockContracts);

            ContractListResponse response = contractService.listContracts(
                    200L, "EMPLOYER", List.of("IN_PROGRESS"), null, 1, 10);

            assertEquals(1, response.getItems().size());
            assertEquals("IN_PROGRESS", response.getItems().get(0).getStatus());
        }

        @Test
        @DisplayName("프로젝트명으로 검색할 수 있다")
        void searchesByProjectName() {
            when(contractRepository.findByEmployerIdOrderByIdDesc(200L))
                    .thenReturn(mockContracts);

            ContractListResponse response = contractService.listContracts(
                    200L, "EMPLOYER", null, "프로젝트A", 1, 10);

            assertEquals(1, response.getItems().size());
            assertEquals("프로젝트A", response.getItems().get(0).getProjectName());
        }

        @Test
        @DisplayName("페이지네이션이 올바르게 동작한다")
        void paginatesCorrectly() {
            when(contractRepository.findByEmployerIdOrderByIdDesc(200L))
                    .thenReturn(mockContracts);

            ContractListResponse response = contractService.listContracts(
                    200L, "EMPLOYER", null, null, 1, 1);

            assertEquals(1, response.getItems().size());
            PaginationInfo pagination = response.getPagination();
            assertEquals(1, pagination.page());
            assertEquals(1, pagination.limit());
            assertEquals(2, pagination.total());
            assertEquals(2, pagination.totalPages());
        }

        @Test
        @DisplayName("서명 상태가 올바르게 반환된다")
        void returnsSignatureStatus() {
            when(contractRepository.findByEmployerIdOrderByIdDesc(200L))
                    .thenReturn(mockContracts);

            ContractListResponse response = contractService.listContracts(
                    200L, "EMPLOYER", null, null, 1, 10);

            ContractSummary unsigned = response.getItems().get(0);
            assertFalse(unsigned.getEmployerSigned());
            assertFalse(unsigned.getFreelancerSigned());

            ContractSummary signed = response.getItems().get(1);
            assertTrue(signed.getEmployerSigned());
            assertTrue(signed.getFreelancerSigned());
        }
    }

    @Nested
    @DisplayName("getContract()")
    class GetContract {

        @Test
        @DisplayName("contractId로 계약을 조회할 수 있다")
        void getsContractByContractId() {
            when(contractRepository.findByContractId(1001L))
                    .thenReturn(Optional.of(savedContract));

            ContractResponse response = contractService.getContract(1001L, 200L);

            assertNotNull(response);
            assertEquals(1001L, response.getContractId());
            assertEquals("테스트 프로젝트", response.getProjectName());
        }

        @Test
        @DisplayName("존재하지 않는 계약 조회 시 CONTRACT_NOT_FOUND 에러가 발생한다")
        void throwsErrorWhenContractNotFound() {
            when(contractRepository.findByContractId(9999L))
                    .thenReturn(Optional.empty());

            BusinessException exception = assertThrows(
                    BusinessException.class,
                    () -> contractService.getContract(9999L, 200L)
            );

            assertEquals(ErrorCode.CONTRACT_NOT_FOUND, exception.getErrorCode());
        }

        @Test
        @DisplayName("계약 당사자가 아닌 사용자가 조회 시 CONTRACT_FORBIDDEN 에러가 발생한다")
        void throwsErrorWhenNotOwner() {
            when(contractRepository.findByContractId(1001L))
                    .thenReturn(Optional.of(savedContract)); // employerId=200, freelancerId=100

            BusinessException exception = assertThrows(
                    BusinessException.class,
                    () -> contractService.getContract(1001L, 999L) // 무관한 사용자
            );

            assertEquals(ErrorCode.CONTRACT_FORBIDDEN, exception.getErrorCode());
        }
    }

    @Nested
    @DisplayName("sign()")
    class Sign {

        @Test
        @DisplayName("프리랜서가 서명하면 freelancerSignature가 설정된다")
        void freelancerSignsSetsSignature() {
            savedContract.setEmployerSignature("emp_sig");
            when(contractRepository.findByContractId(1001L))
                    .thenReturn(Optional.of(savedContract));
            when(contractRepository.save(any(Contract.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            ContractResponse response = contractService.sign(1001L, signRequest("free_sig"), "FREELANCER", 100L);

            assertNotNull(response.getFreelancerSignature());
            assertEquals("free_sig", response.getFreelancerSignature());
        }

        @Test
        @DisplayName("양측 서명 완료 시 상태가 IN_PROGRESS로 변경된다")
        void bothSignaturesActivatesContract() {
            savedContract.setStatus(ContractStatus.WAITING_SIGNATURE);
            savedContract.setEmployerSignature("emp_sig");

            when(contractRepository.findByContractId(1001L))
                    .thenReturn(Optional.of(savedContract));
            when(contractRepository.save(any(Contract.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));
            when(contractPdfService.generateSignedPdf(any(Contract.class)))
                    .thenReturn("/pdfs/contracts/1001_signed.pdf");

            ContractResponse response = contractService.sign(1001L, signRequest("free_sig"), "FREELANCER", 100L);

            assertEquals("IN_PROGRESS", response.getStatus());
            assertNotNull(response.getSignedDate());
        }

        @Test
        @DisplayName("양측 서명 완료 시 서명된 PDF가 생성된다")
        void bothSignaturesGeneratesSignedPdf() {
            savedContract.setStatus(ContractStatus.WAITING_SIGNATURE);
            savedContract.setEmployerSignature("emp_sig");

            when(contractRepository.findByContractId(1001L))
                    .thenReturn(Optional.of(savedContract));
            when(contractRepository.save(any(Contract.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));
            when(contractPdfService.generateSignedPdf(any(Contract.class)))
                    .thenReturn("/pdfs/contracts/1001_signed.pdf");

            ContractResponse response = contractService.sign(1001L, signRequest("free_sig"), "FREELANCER", 100L);

            verify(contractPdfService).generateSignedPdf(any(Contract.class));
            assertEquals("/pdfs/contracts/1001_signed.pdf", response.getSignedPdfUrl());
        }

        @Test
        @DisplayName("양측 서명 완료 시 ContractActivatedEvent가 발행된다")
        void bothSignaturesPublishesEvent() {
            savedContract.setStatus(ContractStatus.WAITING_SIGNATURE);
            savedContract.setEmployerSignature("emp_sig");

            when(contractRepository.findByContractId(1001L))
                    .thenReturn(Optional.of(savedContract));
            when(contractRepository.save(any(Contract.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));
            when(contractPdfService.generateSignedPdf(any(Contract.class)))
                    .thenReturn("/pdfs/contracts/1001_signed.pdf");

            contractService.sign(1001L, signRequest("free_sig"), "FREELANCER", 100L);

            verify(eventPublisher).publishEvent(any(ContractActivatedEvent.class));
        }

        @Test
        @DisplayName("한쪽만 서명한 경우 이벤트가 발행되지 않는다")
        void onlyOneSignatureDoesNotPublishEvent() {
            when(contractRepository.findByContractId(1001L))
                    .thenReturn(Optional.of(savedContract));
            when(contractRepository.save(any(Contract.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            contractService.sign(1001L, signRequest("free_sig"), "FREELANCER", 100L);

            verify(eventPublisher, never()).publishEvent(any());
        }

        @Test
        @DisplayName("존재하지 않는 계약 서명 시 CONTRACT_NOT_FOUND 에러가 발생한다")
        void throwsErrorWhenContractNotFound() {
            when(contractRepository.findByContractId(9999L))
                    .thenReturn(Optional.empty());

            BusinessException exception = assertThrows(
                    BusinessException.class,
                    () -> contractService.sign(9999L, signRequest("sig"), "FREELANCER", 100L)
            );

            assertEquals(ErrorCode.CONTRACT_NOT_FOUND, exception.getErrorCode());
        }

        @Test
        @DisplayName("계약 당사자가 아닌 사용자가 서명 시 CONTRACT_FORBIDDEN 에러가 발생한다")
        void throwsErrorWhenNotOwner() {
            when(contractRepository.findByContractId(1001L))
                    .thenReturn(Optional.of(savedContract)); // employerId=200, freelancerId=100

            BusinessException exception = assertThrows(
                    BusinessException.class,
                    () -> contractService.sign(1001L, signRequest("sig"), "FREELANCER", 999L) // 무관한 사용자
            );

            assertEquals(ErrorCode.CONTRACT_FORBIDDEN, exception.getErrorCode());
        }

        @Test
        @DisplayName("유효하지 않은 role로 서명 시 CONTRACT_FORBIDDEN 에러가 발생한다")
        void throwsErrorWhenInvalidRole() {
            when(contractRepository.findByContractId(1001L))
                    .thenReturn(Optional.of(savedContract));

            BusinessException exception = assertThrows(
                    BusinessException.class,
                    () -> contractService.sign(1001L, signRequest("sig"), "ADMIN", 100L)
            );

            assertEquals(ErrorCode.CONTRACT_FORBIDDEN, exception.getErrorCode());
        }
    }

    @Nested
    @DisplayName("complete()")
    class Complete {

        @Test
        @DisplayName("IN_PROGRESS 상태의 계약을 완료 처리할 수 있다")
        void completesInProgressContract() {
            savedContract.setStatus(ContractStatus.IN_PROGRESS);

            when(contractRepository.findByContractId(1001L))
                    .thenReturn(Optional.of(savedContract));
            when(contractRepository.save(any(Contract.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            ContractResponse response = contractService.complete(1001L, 200L);

            assertEquals("COMPLETED", response.getStatus());
        }

        @Test
        @DisplayName("IN_PROGRESS가 아닌 계약 완료 시 CONTRACT_NOT_IN_PROGRESS 에러가 발생한다")
        void throwsErrorWhenNotInProgress() {
            savedContract.setStatus(ContractStatus.WAITING_SIGNATURE);

            when(contractRepository.findByContractId(1001L))
                    .thenReturn(Optional.of(savedContract));

            BusinessException exception = assertThrows(
                    BusinessException.class,
                    () -> contractService.complete(1001L, 200L)
            );

            assertEquals(ErrorCode.CONTRACT_NOT_IN_PROGRESS, exception.getErrorCode());
        }

        @Test
        @DisplayName("존재하지 않는 계약 완료 시 CONTRACT_NOT_FOUND 에러가 발생한다")
        void throwsErrorWhenContractNotFound() {
            when(contractRepository.findByContractId(9999L))
                    .thenReturn(Optional.empty());

            BusinessException exception = assertThrows(
                    BusinessException.class,
                    () -> contractService.complete(9999L, 200L)
            );

            assertEquals(ErrorCode.CONTRACT_NOT_FOUND, exception.getErrorCode());
        }

        @Test
        @DisplayName("계약 당사자가 아닌 사용자가 완료 처리 시 CONTRACT_FORBIDDEN 에러가 발생한다")
        void throwsErrorWhenNotOwner() {
            savedContract.setStatus(ContractStatus.IN_PROGRESS);

            when(contractRepository.findByContractId(1001L))
                    .thenReturn(Optional.of(savedContract));

            BusinessException exception = assertThrows(
                    BusinessException.class,
                    () -> contractService.complete(1001L, 999L) // 무관한 사용자
            );

            assertEquals(ErrorCode.CONTRACT_FORBIDDEN, exception.getErrorCode());
        }
    }

    @Nested
    @DisplayName("reject()")
    class Reject {

        @Test
        @DisplayName("WAITING_SIGNATURE 상태의 계약을 거부할 수 있다")
        void rejectsWaitingContract() {
            savedContract.setStatus(ContractStatus.WAITING_SIGNATURE);

            when(contractRepository.findByContractId(1001L))
                    .thenReturn(Optional.of(savedContract));
            when(contractRepository.save(any(Contract.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            ContractResponse response = contractService.reject(1001L, 200L);

            assertEquals("REJECTED", response.getStatus());
        }

        @Test
        @DisplayName("IN_PROGRESS 상태의 계약 거부 시 CONTRACT_CANNOT_REJECT 에러가 발생한다")
        void throwsErrorWhenInProgress() {
            savedContract.setStatus(ContractStatus.IN_PROGRESS);

            when(contractRepository.findByContractId(1001L))
                    .thenReturn(Optional.of(savedContract));

            BusinessException exception = assertThrows(
                    BusinessException.class,
                    () -> contractService.reject(1001L, 200L)
            );

            assertEquals(ErrorCode.CONTRACT_CANNOT_REJECT, exception.getErrorCode());
        }

        @Test
        @DisplayName("COMPLETED 상태의 계약 거부 시 CONTRACT_CANNOT_REJECT 에러가 발생한다")
        void throwsErrorWhenCompleted() {
            savedContract.setStatus(ContractStatus.COMPLETED);

            when(contractRepository.findByContractId(1001L))
                    .thenReturn(Optional.of(savedContract));

            BusinessException exception = assertThrows(
                    BusinessException.class,
                    () -> contractService.reject(1001L, 200L)
            );

            assertEquals(ErrorCode.CONTRACT_CANNOT_REJECT, exception.getErrorCode());
        }

        @Test
        @DisplayName("존재하지 않는 계약 거절 시 CONTRACT_NOT_FOUND 에러가 발생한다")
        void throwsErrorWhenContractNotFound() {
            when(contractRepository.findByContractId(9999L))
                    .thenReturn(Optional.empty());

            BusinessException exception = assertThrows(
                    BusinessException.class,
                    () -> contractService.reject(9999L, 200L)
            );

            assertEquals(ErrorCode.CONTRACT_NOT_FOUND, exception.getErrorCode());
        }

        @Test
        @DisplayName("계약 당사자가 아닌 사용자가 거절 시 CONTRACT_FORBIDDEN 에러가 발생한다")
        void throwsErrorWhenNotOwner() {
            savedContract.setStatus(ContractStatus.WAITING_SIGNATURE);

            when(contractRepository.findByContractId(1001L))
                    .thenReturn(Optional.of(savedContract));

            BusinessException exception = assertThrows(
                    BusinessException.class,
                    () -> contractService.reject(1001L, 999L) // 무관한 사용자
            );

            assertEquals(ErrorCode.CONTRACT_FORBIDDEN, exception.getErrorCode());
        }
    }
}
