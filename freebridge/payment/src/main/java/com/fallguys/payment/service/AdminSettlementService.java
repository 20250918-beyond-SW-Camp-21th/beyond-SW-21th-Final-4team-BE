package com.fallguys.payment.service;

import com.fallguys.common.exception.BusinessException;
import com.fallguys.common.exception.ErrorCode;
import com.fallguys.common.api.contract.ContractInfo;
import com.fallguys.common.api.contract.ContractQuery;
import com.fallguys.payment.api.web.dto.*;
import com.fallguys.payment.entity.*;
import com.fallguys.payment.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminSettlementService {

    private final EmployerSettlementRepository employerSettlementRepository;
    private final FreelancerSettlementRepository freelancerSettlementRepository;
    private final WalletRepository walletRepository;
    private final ContractQuery contractQuery;
    private final EmployerSettlementService employerSettlementService;
    private final AdminSettlementDisbursementService adminSettlementDisbursementService;

    /**
     * 계약 정산 레코드 수동 생성 (포트원 검증 없이, 어드민/테스트 용도)
     */
    @Transactional
    public void generateSettlements(Long contractId) {
        ContractInfo contract = contractQuery.getContractInfo(contractId);

        List<EmployerSettlement> existing = employerSettlementRepository.findByContractId(contractId);
        if (!existing.isEmpty()) {
            log.warn("계약 #{} 에 대한 정산 레코드가 이미 존재합니다. 건너뜁니다.", contractId);
            return;
        }

        employerSettlementService.createSettlementRecords(contract, "MANUAL-" + contractId,
                contract.employerId());
        log.info("계약 #{} 정산 레코드 수동 생성 완료", contractId);
    }

    /**
     * 프리랜서 월별 정산 실행
     * scheduledDate <= 오늘 이고 PENDING 상태인 FreelancerSettlement를 처리
     * 
     * [Note] REQUIRES_NEW 트랜잭션을 사용하는 processSingleDisbursement 호출을 위해
     * 이 메서드는 비트랜잭션이거나 읽기 전용이어야 데드락을 방지할 수 있습니다.
     */
    public void runDisbursement() {
        LocalDate today = LocalDate.now();
        List<FreelancerSettlement> pendingList = freelancerSettlementRepository
                .findByStatusAndScheduledDateLessThanEqual(FreelancerSettlementStatus.PENDING, today);

        if (pendingList.isEmpty()) {
            log.info("지급 대상 정산 없음 (기준일: {})", today);
            return;
        }

        int successCount = 0;
        for (FreelancerSettlement fs : pendingList) {
            try {
                adminSettlementDisbursementService.processSingleDisbursement(fs.getId());
                successCount++;
            } catch (Exception e) {
                log.error("정산 처리 실패: freelancerSettlementId={}, error={}", fs.getId(), e.getMessage());
            }
        }

        log.info("정산 실행 완료: 성공={}/{}", successCount, pendingList.size());
    }

    @Transactional(readOnly = true)
    public PageResponse<EmployerSettlementItem> listAllSettlements(String status, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(0, page - 1), size,
                Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<EmployerSettlement> pageResult;

        if (!"ALL".equalsIgnoreCase(status)) {
            EmployerSettlementStatus statusEnum;
            try {
                statusEnum = EmployerSettlementStatus.valueOf(status != null ? status.trim().toUpperCase() : "");
            } catch (IllegalArgumentException e) {
                throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
            }
            pageResult = employerSettlementRepository.findByStatus(statusEnum, pageable);
        } else {
            pageResult = employerSettlementRepository.findAll(pageable);
        }

        List<EmployerSettlementItem> items = pageResult.getContent().stream()
                .map(e -> new EmployerSettlementItem(
                        e.getId(), e.getContractId(), null, null,
                        e.getBillingAmount(), e.getPlatformFee(), e.getTotalPayment(),
                        e.getInstallmentNumber(), e.getStatus().name(),
                        e.getInvoicePdfUrl(), e.getDueDate(), e.getPaidDate()))
                .toList();

        return new PageResponse<>(items, pageResult.getTotalElements(),
                pageResult.getTotalPages(), page);
    }
}
