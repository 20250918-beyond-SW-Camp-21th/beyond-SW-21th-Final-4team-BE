package com.fallguys.payment.service.impl;

import com.fallguys.common.exception.BusinessException;
import com.fallguys.common.exception.ErrorCode;
import com.fallguys.contract.api.shared.ContractInfo;
import com.fallguys.contract.api.shared.ContractQuery;
import com.fallguys.payment.api.web.dto.*;
import com.fallguys.payment.entity.*;
import com.fallguys.payment.portone.PortOneApiClient;
import com.fallguys.payment.portone.PortOnePaymentInfo;
import com.fallguys.payment.repository.*;
import com.fallguys.payment.service.EmployerSettlementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
public class EmployerSettlementServiceImpl implements EmployerSettlementService {

    private final EmployerSettlementRepository employerSettlementRepository;
    private final FreelancerSettlementRepository freelancerSettlementRepository;
    private final WalletRepository walletRepository;
    private final WalletTransactionRepository walletTransactionRepository;
    private final ContractQuery contractQuery;
    private final PortOneApiClient portOneApiClient;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<EmployerSettlementItem> listSettlements(
            Long employerId, String status, String dateRange,
            String search, String sort, int page, int size) {

        Pageable pageable = buildPageable(sort, page, size);
        Page<EmployerSettlement> pageResult;

        if (!"ALL".equalsIgnoreCase(status)) {
            EmployerSettlementStatus statusEnum = EmployerSettlementStatus.valueOf(status);
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

    @Override
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

    @Override
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

    @Override
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

    @Override
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
    @Override
    @Transactional
    public VerifyPaymentResponse verifyContractPayment(String paymentId, Long contractId, Long employerId) {

        // 멱등성 체크: 동일 paymentId 재호출 시 기존 결과 반환
        if (employerSettlementRepository.existsByTransactionId(paymentId)) {
            List<EmployerSettlement> existing = employerSettlementRepository.findByContractId(contractId);
            long totalVerified = existing.stream().mapToLong(EmployerSettlement::getTotalPayment).sum();
            return new VerifyPaymentResponse(true, contractId, totalVerified, existing.size());
        }

        // 포트원 V2 결제 검증
        PortOnePaymentInfo payment = portOneApiClient.getPayment(paymentId);

        if (!payment.isPaid()) {
            throw new BusinessException(ErrorCode.PAYMENT_FAILED);
        }

        // 계약 정보 조회
        ContractInfo contract = contractQuery.getContractInfo(contractId);

        // 회차별 정산 레코드 생성
        List<EmployerSettlement> settlements = createSettlementRecords(contract, paymentId, employerId);

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

        // 고용주 지갑 데빗 기록 (가상)
        Wallet employerWallet = getOrCreateUserWallet(employerId, WalletType.EMPLOYER);
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
     * 계약 정산 레코드 생성 (AdminSettlementServiceImpl에서도 사용)
     */
    public List<EmployerSettlement> createSettlementRecords(ContractInfo contract, String paymentId, Long employerId) {
        LocalDate startDate = contract.startDate();
        LocalDate endDate = contract.endDate();
        long budget = contract.budget();
        double commissionRate = contract.commissionRate() != null ? contract.commissionRate() : 0.0;
        int paymentDay = contract.paymentDay() != null ? contract.paymentDay() : startDate.getDayOfMonth();

        int totalMonths = (int) ChronoUnit.MONTHS.between(
                startDate.withDayOfMonth(1), endDate.withDayOfMonth(1)) + 1;
        if (totalMonths < 1) totalMonths = 1;

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
            es.setTransactionId(paymentId);
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

    Wallet getOrCreatePlatformWallet(WalletType walletType) {
        return walletRepository.findByWalletType(walletType)
                .orElseGet(() -> {
                    Wallet w = new Wallet();
                    w.setWalletType(walletType);
                    w.setBalance(0L);
                    return walletRepository.save(w);
                });
    }

    Wallet getOrCreateUserWallet(Long ownerId, WalletType walletType) {
        return walletRepository.findByOwnerIdAndWalletType(ownerId, walletType)
                .orElseGet(() -> {
                    Wallet w = new Wallet();
                    w.setOwnerId(ownerId);
                    w.setWalletType(walletType);
                    w.setBalance(0L);
                    return walletRepository.save(w);
                });
    }

    private Pageable buildPageable(String sort, int page, int size) {
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
            case "LAST_3_MONTHS" -> new LocalDate[]{now.minusMonths(3), now};
            case "LAST_6_MONTHS" -> new LocalDate[]{now.minusMonths(6), now};
            case "LAST_1_YEAR" -> new LocalDate[]{now.minusYears(1), now};
            default -> new LocalDate[]{LocalDate.of(2000, 1, 1), now};
        };
    }
}
