package com.fallguys.payment.service;

import com.fallguys.common.exception.BusinessException;
import com.fallguys.common.exception.ErrorCode;
import com.fallguys.common.api.contract.ContractInfo;
import com.fallguys.common.api.contract.ContractQuery;
import com.fallguys.payment.api.web.dto.*;
import com.fallguys.payment.entity.*;
import com.fallguys.payment.portone.PortOneApiClient;
import com.fallguys.payment.portone.PortOnePaymentInfo;
import com.fallguys.payment.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmployerSettlementService {

    private final EmployerSettlementRepository employerSettlementRepository;
    private final FreelancerSettlementRepository freelancerSettlementRepository;
    private final WalletRepository walletRepository;
    private final WalletTransactionRepository walletTransactionRepository;
    private final ContractQuery contractQuery;
    private final PortOneApiClient portOneApiClient;

    @Transactional(readOnly = true)
    public PageResponse<EmployerSettlementItem> listSettlements(
            Long employerId, String status, String dateRange,
            String sort, int page, int size) {

        // Treat null or blank as "ALL" to avoid valueOf("") blowing up with INVALID_INPUT_VALUE
        if (status == null || status.isBlank()) {
            status = "ALL";
        }
        // Normalize dateRange — a null value would cause parseDateRange() to throw NPE
        if (dateRange == null || dateRange.isBlank()) {
            dateRange = "ALL";
        }

        Pageable pageable = buildPageable(sort, page, size);
        Page<EmployerSettlement> pageResult;

        if (!"ALL".equalsIgnoreCase(status)) {
            EmployerSettlementStatus statusEnum;
            try {
                statusEnum = EmployerSettlementStatus.valueOf(status.trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
            }
            pageResult = employerSettlementRepository.findByEmployerIdAndStatus(employerId, statusEnum, pageable);
        } else if (!"ALL".equalsIgnoreCase(dateRange)) {
            LocalDate[] range = parseDateRange(dateRange);
            pageResult = employerSettlementRepository.findByEmployerIdAndDueDateBetween(
                    employerId, range[0], range[1], pageable);
        } else {
            pageResult = employerSettlementRepository.findByEmployerId(employerId, pageable);
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

    @Transactional(readOnly = true)
    public EmployerSettlementSummaryResponse getSummary(Long employerId) {
        Long totalPaid = employerSettlementRepository
                .sumTotalPaymentByEmployerIdAndStatusPaid(employerId);
        Long totalDisbursed = employerSettlementRepository
                .sumTotalPaymentByEmployerIdAndStatusDisbursed(employerId);
        Integer paidCount = employerSettlementRepository
                .countByEmployerIdAndStatus(employerId, EmployerSettlementStatus.PAID);
        Integer disbursedCount = employerSettlementRepository
                .countByEmployerIdAndStatus(employerId, EmployerSettlementStatus.DISBURSED);
        Integer cancelledCount = employerSettlementRepository
                .countByEmployerIdAndStatus(employerId, EmployerSettlementStatus.CANCELLED);

        return new EmployerSettlementSummaryResponse(
                totalPaid, totalDisbursed, paidCount, disbursedCount, cancelledCount);
    }

    @Transactional(readOnly = true)
    public EmployerSettlementNextResponse getNextSettlement(Long employerId) {
        Pageable top1 = PageRequest.of(0, 1);
        List<EmployerSettlement> list = employerSettlementRepository
                .findFirstPaidByEmployerId(employerId, top1);

        if (list.isEmpty()) {
            return null;
        }
        EmployerSettlement e = list.get(0);
        return new EmployerSettlementNextResponse(
                e.getId(), e.getContractId(), null, null,
                e.getBillingAmount(), e.getPlatformFee(), e.getTotalPayment(),
                e.getInstallmentNumber(), e.getDueDate(), e.getStatus().name());
    }

    @Transactional(readOnly = true)
    public EmployerSettlementDetailResponse getSettlementDetail(Long employerId, Long settlementId) {
        EmployerSettlement e = employerSettlementRepository.findById(settlementId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SETTLEMENT_NOT_FOUND));

        if (!e.getEmployerId().equals(employerId)) {
            throw new BusinessException(ErrorCode.SETTLEMENT_FORBIDDEN);
        }

        ContractInfo contract = contractQuery.getContractInfo(e.getContractId());

        return new EmployerSettlementDetailResponse(
                e.getId(), e.getContractId(), contract.projectName(), null,
                e.getBillingAmount(), e.getPlatformFee(),
                contract.commissionRate(), e.getTotalPayment(),
                e.getInstallmentNumber(), e.getStatus().name(),
                e.getInvoicePdfUrl(), e.getDueDate(), e.getPaidDate());
    }

    @Transactional(readOnly = true)
    public String getInvoicePdfUrl(Long employerId, Long settlementId) {
        EmployerSettlement e = employerSettlementRepository.findById(settlementId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SETTLEMENT_NOT_FOUND));

        if (!e.getEmployerId().equals(employerId)) {
            throw new BusinessException(ErrorCode.SETTLEMENT_FORBIDDEN);
        }
        return e.getInvoicePdfUrl();
    }

    /**
     * PortOne V2 결제 검증 후 정산 레코드 생성 및 에스크로 지갑 처리
     */
    @Transactional
    public VerifyPaymentResponse verifyContractPayment(String paymentId, Long contractId, Long employerId) {

        // 멱등성 체크: 동일 paymentId 재호출 시 기존 결과 반환
        if (employerSettlementRepository.existsByTransactionId(paymentId)) {
            Optional<EmployerSettlement> byTxn = employerSettlementRepository.findByTransactionId(paymentId);
            if (byTxn.isPresent()) {
                EmployerSettlement existing = byTxn.get();
                // 다른 employer가 동일 paymentId로 타인 정산 데이터를 조회하는 것을 방지
                if (!existing.getEmployerId().equals(employerId)) {
                    throw new BusinessException(ErrorCode.SETTLEMENT_FORBIDDEN);
                }
                Long canonicalContractId = existing.getContractId();
                // paymentId가 다른 계약에 이미 사용된 경우 재사용 방지 (cross-contract reuse attack)
                if (!canonicalContractId.equals(contractId)) {
                    log.warn("paymentId 재사용 시도 차단: paymentId={}, 요청 contractId={}, 실제 contractId={}",
                            paymentId, contractId, canonicalContractId);
                    throw new BusinessException(ErrorCode.PAYMENT_FAILED);
                }
                List<EmployerSettlement> allSettlements = employerSettlementRepository.findByContractId(canonicalContractId);
                long totalVerified = allSettlements.stream().mapToLong(EmployerSettlement::getTotalPayment).sum();
                return new VerifyPaymentResponse(true, canonicalContractId, totalVerified, allSettlements.size());
            }
            // Race condition: existsByTransactionId returned true but row disappeared — fall through to re-process
        }

        // 포트원 V2 결제 검증
        PortOnePaymentInfo payment = portOneApiClient.getPayment(paymentId);

        if (!payment.isPaid()) {
            throw new BusinessException(ErrorCode.PAYMENT_FAILED);
        }

        // 포트원에 저장된 customData(contractId/employerId)와 요청값 교차 검증 (위변조 방지)
        PortOnePaymentInfo.CustomDataInfo customData = payment.getCustomData();
        if (customData == null
                || !contractId.equals(customData.getContractId())
                || !employerId.equals(customData.getEmployerId())) {
            log.warn("결제 customData 불일치: paymentId={}, 요청 contractId={}/employerId={}, 실제={}",
                    paymentId, contractId, employerId, customData);
            throw new BusinessException(ErrorCode.PAYMENT_FAILED);
        }

        // 계약 정보 조회
        ContractInfo contract = contractQuery.getContractInfo(contractId);

        // 회차별 정산 레코드 생성
        List<EmployerSettlement> settlements;
        try {
            settlements = createSettlementRecords(contract, paymentId, employerId);
        } catch (DataIntegrityViolationException e) {
            // 동시 요청이 이미 정산 레코드를 생성 완료한 경우 — 지갑 이중 처리를 막기 위해 즉시 반환
            log.info("동시 정산 생성 감지 - 지갑 처리 없이 기존 정산 레코드 반환: paymentId={}", paymentId);
            List<EmployerSettlement> existing = employerSettlementRepository.findByContractId(contractId);
            long total = existing.stream().mapToLong(EmployerSettlement::getTotalPayment).sum();
            return new VerifyPaymentResponse(true, contractId, total, existing.size());
        }

        // 금액 검증: PortOne 결제 금액 == 전체 회차 totalPayment 합산
        long totalExpected = settlements.stream().mapToLong(EmployerSettlement::getTotalPayment).sum();

        if (payment.getTotalAmount() != totalExpected) {
            log.warn("결제 금액 불일치: portone={}, expected={}, contractId={}",
                    payment.getTotalAmount(), totalExpected, contractId);
            throw new BusinessException(ErrorCode.PAYMENT_AMOUNT_MISMATCH);
        }

        // PLATFORM_ESCROW 지갑 크레딧
        Wallet escrowWallet = getOrCreatePlatformWallet(WalletType.PLATFORM_ESCROW);
        escrowWallet.credit(totalExpected);
        walletRepository.save(escrowWallet);

        // 고용주 지갑 데빗: balance 차감 후 저장해야 balanceAfter 스냅샷도 정확해짐
        Wallet employerWallet = getOrCreateUserWallet(employerId, WalletType.EMPLOYER);
        if (employerWallet.getBalance() < totalExpected) {
            log.warn("고용주 지갑 잔액 부족: employerId={}, balance={}, required={}",
                    employerId, employerWallet.getBalance(), totalExpected);
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
        employerWallet.debit(totalExpected);
        walletRepository.save(employerWallet);
        walletTransactionRepository.save(new WalletTransaction(
                employerWallet.getId(), TransactionType.DEBIT, totalExpected,
                TransactionReferenceType.CONTRACT_PAYMENT, contractId,
                "계약금 결제 (계약 #" + contractId + ")", employerWallet.getBalance()));

        // ESCROW 크레딧 트랜잭션 기록
        walletTransactionRepository.save(new WalletTransaction(
                escrowWallet.getId(), TransactionType.CREDIT, totalExpected,
                TransactionReferenceType.CONTRACT_PAYMENT, contractId,
                "계약 에스크로 입금 (계약 #" + contractId + ")", escrowWallet.getBalance()));

        log.info("계약 결제 검증 완료: paymentId={}, contractId={}, installments={}, total={}",
                paymentId, contractId, settlements.size(), totalExpected);

        return new VerifyPaymentResponse(true, contractId, totalExpected, settlements.size());
    }

    /**
     * 계약 정산 레코드 생성 (AdminSettlementService에서도 사용)
     */
    @Transactional
    public List<EmployerSettlement> createSettlementRecords(ContractInfo contract, String paymentId, Long employerId) {
        LocalDate startDate = contract.startDate();
        LocalDate endDate = contract.endDate();
        long budget = contract.budget();
        double commissionRate = contract.commissionRate() != null ? contract.commissionRate() : 0.0;
        int paymentDay = contract.paymentDay() != null ? contract.paymentDay() : startDate.getDayOfMonth();

        int totalMonths = (int) ChronoUnit.MONTHS.between(
                startDate.withDayOfMonth(1), endDate.withDayOfMonth(1)) + 1;
        if (totalMonths < 1)
            totalMonths = 1;

        long baseInstallment = budget / totalMonths;
        List<EmployerSettlement> result = new ArrayList<>();

        for (int i = 1; i <= totalMonths; i++) {
            long billingAmount = (i < totalMonths)
                    ? baseInstallment
                    : budget - baseInstallment * (totalMonths - 1);

            long platformFee = (long) (billingAmount * commissionRate);
            long totalPayment = billingAmount + platformFee;
            LocalDate dueDate = buildDueDate(startDate, i - 1, paymentDay);

            EmployerSettlement es = new EmployerSettlement();
            es.setContractId(contract.id());
            es.setEmployerId(employerId);
            es.setFreelancerId(contract.freelancerId());
            es.setTransactionId((i == 1) ? paymentId : null);
            es.setBillingAmount(billingAmount);
            es.setPlatformFee(platformFee);
            es.setTotalPayment(totalPayment);
            es.setInstallmentNumber(i);
            es.setStatus(EmployerSettlementStatus.PAID);
            es.setPaidDate(LocalDate.now());
            es.setDueDate(dueDate);
            employerSettlementRepository.save(es);

            // 대응하는 FreelancerSettlement 생성
            long fsPlatformFee = (long) (billingAmount * commissionRate);
            long tax = (long) ((billingAmount - fsPlatformFee) * 0.033);
            long netAmount = billingAmount - fsPlatformFee - tax;

            FreelancerSettlement fs = new FreelancerSettlement();
            fs.setContractId(contract.id());
            fs.setEmployerSettlementId(es.getId());
            fs.setFreelancerId(contract.freelancerId());
            fs.setTotalAmount(billingAmount);
            fs.setPlatformFee(fsPlatformFee);
            fs.setTax(tax);
            fs.setNetAmount(netAmount);
            fs.setInstallmentNumber(i);
            fs.setStatus(FreelancerSettlementStatus.PENDING);
            fs.setScheduledDate(dueDate);
            freelancerSettlementRepository.save(fs);

            result.add(es);
        }
        return result;
    }

    private LocalDate buildDueDate(LocalDate startDate, int monthsOffset, int paymentDay) {
        LocalDate base = startDate.plusMonths(monthsOffset);
        int lastDay = base.lengthOfMonth();
        int day = Math.min(paymentDay, lastDay);
        return base.withDayOfMonth(day);
    }

    /*
     * @Transactional(readOnly = true)
     * public RefundPreparation prepareRefund(Long contractId, Long employerId) {
     * List<FreelancerSettlement> pendingFs =
     * freelancerSettlementRepository.findByContractIdAndStatus(contractId,
     * FreelancerSettlementStatus.PENDING);
     * if (pendingFs.isEmpty()) {
     * return null;
     * }
     * 
     * List<EmployerSettlement> relatedEs =
     * employerSettlementRepository.findByContractId(contractId);
     * if (relatedEs.isEmpty()) {
     * throw new BusinessException(ErrorCode.SETTLEMENT_NOT_FOUND);
     * }
     * 
     * if (!relatedEs.get(0).getEmployerId().equals(employerId)) {
     * throw new BusinessException(ErrorCode.SETTLEMENT_FORBIDDEN);
     * }
     * 
     * String paymentId = relatedEs.get(0).getTransactionId();
     * if (paymentId == null) {
     * throw new BusinessException(ErrorCode.SETTLEMENT_NOT_FOUND);
     * }
     * 
     * long refundAmount = 0;
     * List<Long> freelancerSettlementIds = new ArrayList<>();
     * for (FreelancerSettlement fs : pendingFs) {
     * EmployerSettlement es =
     * employerSettlementRepository.findById(fs.getEmployerSettlementId())
     * .orElseThrow(() -> new BusinessException(ErrorCode.SETTLEMENT_NOT_FOUND));
     * refundAmount += es.getTotalPayment();
     * freelancerSettlementIds.add(fs.getId());
     * }
     * 
     * return new RefundPreparation(paymentId, refundAmount,
     * freelancerSettlementIds);
     * }
     * 
     * @Transactional
     * public void markAsCancelPending(List<Long> freelancerSettlementIds) {
     * for (Long fsId : freelancerSettlementIds) {
     * FreelancerSettlement fs = freelancerSettlementRepository.findById(fsId)
     * .orElseThrow(() -> new BusinessException(ErrorCode.SETTLEMENT_NOT_FOUND));
     * EmployerSettlement es =
     * employerSettlementRepository.findById(fs.getEmployerSettlementId())
     * .orElseThrow(() -> new BusinessException(ErrorCode.SETTLEMENT_NOT_FOUND));
     * 
     * fs.setStatus(FreelancerSettlementStatus.CANCEL_PENDING);
     * es.setStatus(EmployerSettlementStatus.CANCEL_PENDING);
     * freelancerSettlementRepository.save(fs);
     * employerSettlementRepository.save(es);
     * }
     * }
     * 
     * @Transactional
     * public void rollbackCancelPending(List<Long> freelancerSettlementIds) {
     * for (Long fsId : freelancerSettlementIds) {
     * FreelancerSettlement fs = freelancerSettlementRepository.findById(fsId)
     * .orElseThrow(() -> new BusinessException(ErrorCode.SETTLEMENT_NOT_FOUND));
     * EmployerSettlement es =
     * employerSettlementRepository.findById(fs.getEmployerSettlementId())
     * .orElseThrow(() -> new BusinessException(ErrorCode.SETTLEMENT_NOT_FOUND));
     * 
     * fs.setStatus(FreelancerSettlementStatus.PENDING);
     * es.setStatus(EmployerSettlementStatus.PAID);
     * freelancerSettlementRepository.save(fs);
     * employerSettlementRepository.save(es);
     * }
     * }
     * 
     * @Transactional
     * public void completeCancelAndRefund(Long contractId, Long employerId,
     * List<Long> freelancerSettlementIds,
     * long refundAmount) {
     * for (Long fsId : freelancerSettlementIds) {
     * FreelancerSettlement fs = freelancerSettlementRepository.findById(fsId)
     * .orElseThrow(() -> new BusinessException(ErrorCode.SETTLEMENT_NOT_FOUND));
     * EmployerSettlement es =
     * employerSettlementRepository.findById(fs.getEmployerSettlementId())
     * .orElseThrow(() -> new BusinessException(ErrorCode.SETTLEMENT_NOT_FOUND));
     * 
     * es.setStatus(EmployerSettlementStatus.CANCELLED);
     * employerSettlementRepository.save(es);
     * 
     * fs.setStatus(FreelancerSettlementStatus.CANCELLED);
     * freelancerSettlementRepository.save(fs);
     * }
     * 
     * // escrow 잔고 차감
     * Wallet escrowWallet = getOrCreatePlatformWallet(WalletType.PLATFORM_ESCROW);
     * escrowWallet.debit(refundAmount);
     * walletRepository.save(escrowWallet);
     * 
     * walletTransactionRepository.save(new WalletTransaction(
     * escrowWallet.getId(), TransactionType.DEBIT, refundAmount,
     * TransactionReferenceType.CONTRACT_PAYMENT, contractId,
     * "계약 취소 환불 - 에스크로 출금 (계약 #" + contractId + ")", escrowWallet.getBalance()));
     * 
     * // 고용주 지갑 credit
     * Wallet employerWallet = getOrCreateUserWallet(employerId,
     * WalletType.EMPLOYER);
     * employerWallet.credit(refundAmount);
     * walletRepository.save(employerWallet);
     * 
     * walletTransactionRepository.save(new WalletTransaction(
     * employerWallet.getId(), TransactionType.CREDIT, refundAmount,
     * TransactionReferenceType.CONTRACT_PAYMENT, contractId,
     * "계약 취소 환불 입금 (계약 #" + contractId + ")", employerWallet.getBalance()));
     * }
     * 
     * public void cancelAndRefund(Long contractId, Long employerId, String reason)
     * {
     * RefundPreparation prep = prepareRefund(contractId, employerId);
     * if (prep == null || prep.refundAmount() <= 0) {
     * return;
     * }
     * 
     * markAsCancelPending(prep.freelancerSettlementIds());
     * 
     * try {
     * portOneApiClient.cancelPayment(prep.paymentId(), prep.refundAmount(),
     * reason);
     * completeCancelAndRefund(contractId, employerId,
     * prep.freelancerSettlementIds(), prep.refundAmount());
     * log.info("계약 취소/환불 완료: paymentId={}, contractId={}, refundAmount={}",
     * prep.paymentId(), contractId, prep.refundAmount());
     * } catch (Exception e) {
     * log.error("계약 취소 환불 외부 API 호출 실패: contractId={}, error={}", contractId,
     * e.getMessage());
     * rollbackCancelPending(prep.freelancerSettlementIds());
     * throw e;
     * }
     * }
     * 
     * public record RefundPreparation(String paymentId, long refundAmount,
     * List<Long> freelancerSettlementIds) {
     * }
     */

    Wallet getOrCreatePlatformWallet(WalletType walletType) {
        return walletRepository.findByWalletTypeWithLock(walletType)
                .orElseGet(() -> {
                    Wallet w = new Wallet();
                    w.setWalletType(walletType);
                    w.setBalance(0L);
                    try {
                        return walletRepository.save(w);
                    } catch (DataIntegrityViolationException e) {
                        return walletRepository.findByWalletTypeWithLock(walletType)
                                .orElseThrow(() -> new BusinessException(ErrorCode.WALLET_NOT_FOUND));
                    }
                });
    }

    Wallet getOrCreateUserWallet(Long ownerId, WalletType walletType) {
        return walletRepository.findByOwnerIdAndWalletTypeWithLock(ownerId, walletType)
                .orElseGet(() -> {
                    Wallet w = new Wallet();
                    w.setOwnerId(ownerId);
                    w.setWalletType(walletType);
                    w.setBalance(0L);
                    try {
                        return walletRepository.save(w);
                    } catch (DataIntegrityViolationException e) {
                        return walletRepository.findByOwnerIdAndWalletTypeWithLock(ownerId, walletType)
                                .orElseThrow(() -> new BusinessException(ErrorCode.WALLET_NOT_FOUND));
                    }
                });
    }

    private Pageable buildPageable(String sort, int page, int size) {
        // Guard against null/blank sort — default to ascending dueDate
        if (sort == null || sort.isBlank()) {
            sort = "DUE_DATE_ASC";
        }
        // Guard against invalid size — fall back to a sensible default
        if (size <= 0) {
            size = 20;
        }
        Sort jpaSort = switch (sort) {
            case "DUE_DATE_DESC" -> Sort.by(Sort.Direction.DESC, "dueDate");
            case "AMOUNT_ASC" -> Sort.by(Sort.Direction.ASC, "totalPayment");
            case "AMOUNT_DESC" -> Sort.by(Sort.Direction.DESC, "totalPayment");
            default -> Sort.by(Sort.Direction.ASC, "dueDate");
        };
        return PageRequest.of(Math.max(0, page - 1), size, jpaSort);
    }

    private LocalDate[] parseDateRange(String dateRange) {
        LocalDate now = LocalDate.now();
        return switch (dateRange) {
            case "LAST_3_MONTHS" -> new LocalDate[] { now.minusMonths(3), now };
            case "LAST_6_MONTHS" -> new LocalDate[] { now.minusMonths(6), now };
            case "LAST_1_YEAR" -> new LocalDate[] { now.minusYears(1), now };
            default -> new LocalDate[] { LocalDate.of(2000, 1, 1), now };
        };
    }
}