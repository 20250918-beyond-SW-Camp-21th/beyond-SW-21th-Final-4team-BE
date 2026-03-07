package com.fallguys.payment.service;

import com.fallguys.common.exception.BusinessException;
import com.fallguys.common.exception.ErrorCode;
import com.fallguys.common.api.contract.ContractInfo;
import com.fallguys.common.api.contract.ContractQuery;
import com.fallguys.payment.api.web.dto.*;
import com.fallguys.payment.entity.FreelancerSettlement;
import com.fallguys.payment.entity.FreelancerSettlementStatus;
import com.fallguys.payment.repository.FreelancerSettlementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FreelancerSettlementService {

    private final FreelancerSettlementRepository freelancerSettlementRepository;
    private final ContractQuery contractQuery;

    @Transactional(readOnly = true)
    public PageResponse<FreelancerSettlementItem> listSettlements(
            Long freelancerId, String status, String dateRange,
            String search, String sort, int page, int size) {

        Pageable pageable = buildPageable(sort, page, size);
        Page<FreelancerSettlement> pageResult;

        if (!"ALL".equalsIgnoreCase(status)) {
            FreelancerSettlementStatus statusEnum = FreelancerSettlementStatus.valueOf(status);
            pageResult = freelancerSettlementRepository.findByFreelancerIdAndStatus(freelancerId, statusEnum, pageable);
        } else {
            pageResult = freelancerSettlementRepository.findByFreelancerId(freelancerId, pageable);
        }

        List<FreelancerSettlementItem> items = pageResult.getContent().stream()
                .map(f -> new FreelancerSettlementItem(
                        f.getId(), f.getContractId(), f.getEmployerSettlementId(),
                        null, null,
                        f.getTotalAmount(), f.getPlatformFee(), f.getTax(), f.getNetAmount(),
                        f.getInstallmentNumber(), f.getStatus().name(),
                        f.getScheduledDate(), f.getPaidDate(), f.getReceiptPdfUrl()))
                .toList();

        return new PageResponse<>(items, pageResult.getTotalElements(),
                pageResult.getTotalPages(), page);
    }

    @Transactional(readOnly = true)
    public FreelancerSettlementSummaryResponse getSummary(Long freelancerId) {
        Long pendingAmount = freelancerSettlementRepository
                .sumNetAmountByFreelancerIdAndStatusPending(freelancerId);
        Long paidAmount = freelancerSettlementRepository
                .sumNetAmountByFreelancerIdAndStatusPaid(freelancerId);
        Integer pendingCount = freelancerSettlementRepository
                .countByFreelancerIdAndStatus(freelancerId, FreelancerSettlementStatus.PENDING);
        Integer paidCount = freelancerSettlementRepository
                .countByFreelancerIdAndStatus(freelancerId, FreelancerSettlementStatus.PAID);

        return new FreelancerSettlementSummaryResponse(pendingAmount, pendingCount, paidAmount, paidCount);
    }

    @Transactional(readOnly = true)
    public FreelancerSettlementDetailResponse getSettlementDetail(Long freelancerId, Long settlementId) {
        FreelancerSettlement f = freelancerSettlementRepository.findById(settlementId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SETTLEMENT_NOT_FOUND));

        if (!f.getFreelancerId().equals(freelancerId)) {
            throw new BusinessException(ErrorCode.SETTLEMENT_FORBIDDEN);
        }

        ContractInfo contract = contractQuery.getContractInfo(f.getContractId());

        return new FreelancerSettlementDetailResponse(
                f.getId(), f.getContractId(), f.getEmployerSettlementId(),
                contract.projectName(), null, contract.paymentDay(),
                f.getTotalAmount(), f.getPlatformFee(), contract.commissionRate(),
                f.getTax(), f.getNetAmount(), f.getInstallmentNumber(),
                f.getStatus().name(), f.getScheduledDate(), f.getPaidDate(), f.getReceiptPdfUrl());
    }

    @Transactional(readOnly = true)
    public String getReceiptPdfUrl(Long freelancerId, Long settlementId) {
        FreelancerSettlement f = freelancerSettlementRepository.findById(settlementId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SETTLEMENT_NOT_FOUND));

        if (!f.getFreelancerId().equals(freelancerId)) {
            throw new BusinessException(ErrorCode.SETTLEMENT_FORBIDDEN);
        }
        return f.getReceiptPdfUrl();
    }

    @Transactional(readOnly = true)
    public TaxInvoiceResponse requestTaxInvoice(Long freelancerId, Long settlementId, TaxInvoiceRequest request) {
        FreelancerSettlement f = freelancerSettlementRepository.findById(settlementId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SETTLEMENT_NOT_FOUND));

        if (!f.getFreelancerId().equals(freelancerId)) {
            throw new BusinessException(ErrorCode.SETTLEMENT_FORBIDDEN);
        }

        if (f.getStatus() != FreelancerSettlementStatus.PAID) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }

        // 세금계산서 발행은 외부 시스템 연동 또는 별도 처리 (현재는 요청 등록만 처리)
        return new TaxInvoiceResponse(null, settlementId, "REQUESTED", LocalDateTime.now());
    }

    private Pageable buildPageable(String sort, int page, int size) {
        Sort jpaSort = switch (sort) {
            case "SCHEDULED_DATE_DESC" -> Sort.by(Sort.Direction.DESC, "scheduledDate");
            case "AMOUNT_ASC" -> Sort.by(Sort.Direction.ASC, "netAmount");
            case "AMOUNT_DESC" -> Sort.by(Sort.Direction.DESC, "netAmount");
            default -> Sort.by(Sort.Direction.ASC, "scheduledDate");
        };
        return PageRequest.of(Math.max(0, page - 1), size, jpaSort);
    }
}