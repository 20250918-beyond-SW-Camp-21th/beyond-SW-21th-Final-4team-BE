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
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
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
    private final PlatformTransactionManager transactionManager;
    private final PaymentInvoicePdfService paymentInvoicePdfService;

    @Transactional(readOnly = true)
    public PageResponse<EmployerSettlementItem> listSettlements(
            Long employerId, String status, String dateRange,
            String sort, int page, int size) {

        // Treat null or blank as "ALL" to avoid valueOf("") blowing up with
        // INVALID_INPUT_VALUE
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

        ContractInfo contract = contractQuery.getContractInfoByContractId(e.getContractId());

        return new EmployerSettlementDetailResponse(
                e.getId(), e.getContractId(), contract.projectName(), null,
                e.getBillingAmount(), e.getPlatformFee(),
                contract.commissionRate(), e.getTotalPayment(),
                e.getInstallmentNumber(), e.getStatus().name(),
                e.getInvoicePdfUrl(), e.getDueDate(), e.getPaidDate());
    }

    @Transactional
    public String getInvoicePdfUrl(Long employerId, Long settlementId) {
        EmployerSettlement e = employerSettlementRepository.findById(settlementId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SETTLEMENT_NOT_FOUND));

        if (!e.getEmployerId().equals(employerId)) {
            throw new BusinessException(ErrorCode.SETTLEMENT_FORBIDDEN);
        }
        if (e.getInvoicePdfUrl() == null) {
            ContractInfo contract = contractQuery.getContractInfoByContractId(e.getContractId());
            String key = paymentInvoicePdfService.generateServiceFeeInvoice(e, contract);
            e.setInvoicePdfUrl(key);
            employerSettlementRepository.save(e);
        }
        return paymentInvoicePdfService.generatePresignedUrl(e.getInvoicePdfUrl());
    }

    @Transactional
    public String regenerateInvoicePdf(Long employerId, Long settlementId) {
        EmployerSettlement e = employerSettlementRepository.findById(settlementId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SETTLEMENT_NOT_FOUND));

        if (!e.getEmployerId().equals(employerId)) {
            throw new BusinessException(ErrorCode.SETTLEMENT_FORBIDDEN);
        }

        ContractInfo contract = contractQuery.getContractInfoByContractId(e.getContractId());
        String invoiceUrl = paymentInvoicePdfService.generateServiceFeeInvoice(e, contract);
        e.setInvoicePdfUrl(invoiceUrl);
        employerSettlementRepository.save(e);
        return invoiceUrl;
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
                List<EmployerSettlement> allSettlements = employerSettlementRepository
                        .findByContractId(canonicalContractId);
                long totalVerified = allSettlements.stream().mapToLong(EmployerSettlement::getTotalPayment).sum();
                return new VerifyPaymentResponse(true, canonicalContractId, totalVerified, allSettlements.size());
            }
            // Race condition: existsByTransactionId returned true but row disappeared —
            // fall through to re-process
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
        ContractInfo contract = contractQuery.getContractInfoByContractId(contractId);
        validatePayableContract(contract, employerId);

        // 이 지점부터 외부 결제는 PAID 확정 — 내부 처리 실패 시 cancelPayment로 보상
        try {
            // 회차별 정산 레코드 생성
            List<EmployerSettlement> settlements;
            try {
                settlements = createSettlementRecords(contract, paymentId, employerId);
            } catch (DataIntegrityViolationException e) {
                if (isUniqueConstraintViolation(e)) {
                    // 동시 요청이 이미 정산 레코드를 생성한 경우 (유니크 제약 위반).
                    // DIVE 발생 후 트랜잭션이 중단(aborted) 상태이므로 outer catch 전용 핸들러로 처리합니다
                    // (보상 취소를 호출하지 않음 — 다른 요청이 이미 결제를 정상 처리했음).
                    log.info("동시 정산 생성 감지 (유니크 제약 위반) — outer catch로 전파: paymentId={}", paymentId);
                    throw new ConcurrentSettlementException(e);
                }
                // FK, NOT NULL 등 다른 무결성 위반은 보상 취소가 필요한 실제 오류
                log.error("정산 레코드 생성 중 무결성 위반 (유니크 제약 위반 아님) — 보상 취소 필요: paymentId={}, error={}",
                        paymentId, e.getMessage());
                throw e;
            }

            // 금액 검증: PortOne 결제 금액 == 전체 회차 totalPayment 합산
            long totalExpected = settlements.stream().mapToLong(EmployerSettlement::getTotalPayment).sum();

            if (payment.getTotalAmount() != totalExpected) {
                log.warn("결제 금액 불일치: portone={}, expected={}, contractId={}",
                        payment.getTotalAmount(), totalExpected, contractId);
                throw new BusinessException(ErrorCode.PAYMENT_AMOUNT_MISMATCH);
            }

            // 서비스 수수료(platformFee 합산)는 PLATFORM_REVENUE, 나머지(프리랜서 지급액)는 PLATFORM_ESCROW
            long totalPlatformFees = settlements.stream().mapToLong(EmployerSettlement::getPlatformFee).sum();
            long totalEscrowAmount = totalExpected - totalPlatformFees;

            // PLATFORM_ESCROW 지갑 크레딧 (프리랜서 지급 예정액)
            Wallet escrowWallet = getOrCreatePlatformWallet(WalletType.PLATFORM_ESCROW);
            escrowWallet.credit(totalEscrowAmount);
            walletRepository.save(escrowWallet);
            walletTransactionRepository.save(new WalletTransaction(
                    escrowWallet.getId(), TransactionType.CREDIT, totalEscrowAmount,
                    TransactionReferenceType.CONTRACT_PAYMENT, contractId,
                    "계약 에스크로 입금 (계약 #" + contractId + ")", escrowWallet.getBalance()));

            // PLATFORM_REVENUE 지갑 크레딧 (서비스 수수료)
            Wallet revenueWallet = getOrCreatePlatformWallet(WalletType.PLATFORM_REVENUE);
            revenueWallet.credit(totalPlatformFees);
            walletRepository.save(revenueWallet);
            walletTransactionRepository.save(new WalletTransaction(
                    revenueWallet.getId(), TransactionType.CREDIT, totalPlatformFees,
                    TransactionReferenceType.CONTRACT_PAYMENT, contractId,
                    "서비스 수수료 수익 (계약 #" + contractId + ")", revenueWallet.getBalance()));

            // 고용주 지갑: PortOne 결제금 크레딧 후 계약금 데빗
            Wallet employerWallet = getOrCreateUserWallet(employerId, WalletType.EMPLOYER);
            employerWallet.credit(totalExpected);
            walletRepository.save(employerWallet);
            walletTransactionRepository.save(new WalletTransaction(
                    employerWallet.getId(), TransactionType.CREDIT, totalExpected,
                    TransactionReferenceType.CONTRACT_PAYMENT, contractId,
                    "계약금 PortOne 결제 입금 (계약 #" + contractId + ")", employerWallet.getBalance()));

            employerWallet.debit(totalExpected);
            walletRepository.save(employerWallet);
            walletTransactionRepository.save(new WalletTransaction(
                    employerWallet.getId(), TransactionType.DEBIT, totalExpected,
                    TransactionReferenceType.CONTRACT_PAYMENT, contractId,
                    "계약금 결제 (계약 #" + contractId + ")", employerWallet.getBalance()));

            log.info("계약 결제 검증 완료: paymentId={}, contractId={}, installments={}, total={}",
                    paymentId, contractId, settlements.size(), totalExpected);

            return new VerifyPaymentResponse(true, contractId, totalExpected, settlements.size());

        } catch (ConcurrentSettlementException e) {
            // 동시 처리로 인한 정산 레코드 중복 — 포트원 결제를 취소하면 안 됩니다.
            // 현재 트랜잭션은 aborted 상태이므로 별도 트랜잭션에서 기존 정산을 재조회하여 멱등 응답을 반환합니다.
            log.info("동시 처리 감지 — 기존 정산 재조회 시도: paymentId={}, contractId={}", paymentId, contractId);
            try {
                TransactionTemplate newTx = new TransactionTemplate(transactionManager);
                newTx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
                VerifyPaymentResponse idempotentResponse = newTx.execute(status -> {
                    Optional<EmployerSettlement> byTxn = employerSettlementRepository.findByTransactionId(paymentId);
                    if (byTxn.isEmpty())
                        return null;
                    EmployerSettlement existing = byTxn.get();
                    if (!existing.getContractId().equals(contractId))
                        return null;
                    List<EmployerSettlement> allSettlements = employerSettlementRepository.findByContractId(contractId);
                    long total = allSettlements.stream()
                            .mapToLong(EmployerSettlement::getTotalPayment).sum();
                    return new VerifyPaymentResponse(true, contractId, total, allSettlements.size());
                });
                if (idempotentResponse != null) {
                    log.info("동시 처리 — 기존 정산 재조회 성공, 멱등 응답 반환: paymentId={}", paymentId);
                    return idempotentResponse;
                }
            } catch (Exception retryEx) {
                log.warn("동시 처리 — 재조회 실패: paymentId={}, error={}", paymentId, retryEx.getMessage());
            }
            log.warn("동시 처리 — 재조회로 기존 정산 미발견, 예외 전파: paymentId={}", paymentId);
            throw e;
        } catch (Exception e) {
            // 외부 결제가 PAID이지만 내부 처리 실패 — 포트원 결제 취소로 보상 (자금 미포착 상태 유지)
            log.error("결제 후처리 실패, 포트원 결제 취소 보상 시작: paymentId={}, contractId={}, error={}",
                    paymentId, contractId, e.getMessage());
            try {
                portOneApiClient.cancelPayment(paymentId, payment.getTotalAmount(),
                        "내부 처리 실패 자동 보상 취소: " + e.getMessage());
                log.info("포트원 결제 취소 보상 완료: paymentId={}", paymentId);
            } catch (Exception cancelEx) {
                log.error("보상 취소 실패 — 수동 조정 필요: paymentId={}, contractId={}, cancelError={}",
                        paymentId, contractId, cancelEx.getMessage());
            }
            throw (e instanceof RuntimeException re) ? re : new RuntimeException(e);
        }
    }

    /**
     * 계약 정산 레코드 생성 (AdminSettlementService에서도 사용)
     * 결제 가능 계약 검증은 verifyContractPayment에서 수행하며,
     * 관리자 수동 복구 경로는 COMPLETED 계약도 허용해야 하므로 여기서는 수행하지 않는다.
     */
    @Transactional
    public List<EmployerSettlement> createSettlementRecords(ContractInfo contract, String paymentId, Long employerId) {
        Long contractPublicId = contract.contractId();

        if (contractPublicId == null) {
            log.error("계약 business contractId가 null입니다: internalId={}", contract.id());
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }

        // 필수 계약 필드 null 검증 — null이면 NPE 대신 명확한 BusinessException을 던집니다.
        if (contract.budget() == null) {
            log.error("계약 budget이 null입니다: contractId={}", contractPublicId);
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
        if (contract.startDate() == null || contract.endDate() == null) {
            log.error("계약 startDate/endDate가 null입니다: contractId={}, startDate={}, endDate={}",
                    contractPublicId, contract.startDate(), contract.endDate());
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
        if (contract.freelancerId() == null) {
            log.error("계약 freelancerId가 null입니다: contractId={}", contractPublicId);
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }

        LocalDate startDate = contract.startDate();
        LocalDate endDate = contract.endDate();
        long budget = contract.budget();
        // 프론트엔드 기본값(0.05)과 일치 — commissionRate 미설정 계약도 동일하게 처리
        double commissionRate = contract.commissionRate() != null ? contract.commissionRate() : 0.05;
        int paymentDay = contract.paymentDay() != null ? contract.paymentDay() : startDate.getDayOfMonth();

        int totalMonths = (int) ChronoUnit.MONTHS.between(
                startDate.withDayOfMonth(1), endDate.withDayOfMonth(1)) + 1;
        if (totalMonths < 1)
            totalMonths = 1;

        long baseInstallment = budget / totalMonths;
        BigDecimal commissionRateBD = BigDecimal.valueOf(commissionRate);
        BigDecimal taxRateBD = new BigDecimal("0.033");

        // 총 결제금액을 한 번에 계산 — 프론트엔드 Math.round(budget*(1+rate)) 와 동일
        // 회차별 각각 반올림하면 합산 시 오차가 생겨 포트원 금액 검증이 실패함
        long grandTotal = BigDecimal.valueOf(budget)
                .multiply(BigDecimal.ONE.add(commissionRateBD))
                .setScale(0, RoundingMode.HALF_UP)
                .longValue();
        long baseInstallmentTotal = grandTotal / totalMonths;

        List<EmployerSettlement> result = new ArrayList<>();

        for (int i = 1; i <= totalMonths; i++) {
            long billingAmount = (i < totalMonths)
                    ? baseInstallment
                    : budget - baseInstallment * (totalMonths - 1);

            // 회차별 총납부액: 마지막 회차가 반올림 잔액을 흡수 → sum == grandTotal 보장
            long totalPayment = (i < totalMonths)
                    ? baseInstallmentTotal
                    : grandTotal - baseInstallmentTotal * (totalMonths - 1);
            long platformFee = totalPayment - billingAmount;
            LocalDate dueDate = buildDueDate(startDate, i - 1, paymentDay);

            EmployerSettlement es = new EmployerSettlement();
            es.setContractId(contractPublicId);
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

            // 서비스 수수료 인보이스 생성 (실패해도 정산 로직은 계속 진행)
            try {
                String invoiceUrl = paymentInvoicePdfService.generateServiceFeeInvoice(es, contract);
                es.setInvoicePdfUrl(invoiceUrl);
                employerSettlementRepository.save(es);
            } catch (Exception e) {
                log.error("서비스 수수료 인보이스 생성 실패: contractId={}, installment={}, error={}",
                        contractPublicId, i, e.getMessage());
            }

            // 대응하는 FreelancerSettlement 생성
            long fsPlatformFee = platformFee;
            long tax = BigDecimal.valueOf(billingAmount - fsPlatformFee)
                    .multiply(taxRateBD)
                    .setScale(0, RoundingMode.HALF_UP).longValue();
            long netAmount = billingAmount - fsPlatformFee - tax;

            FreelancerSettlement fs = new FreelancerSettlement();
            fs.setContractId(contractPublicId);
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

    private void validatePayableContract(ContractInfo contract, Long employerId) {
        if (contract == null) {
            throw new BusinessException(ErrorCode.CONTRACT_NOT_FOUND);
        }
        if (!employerId.equals(contract.employerId())) {
            throw new BusinessException(ErrorCode.CONTRACT_FORBIDDEN);
        }
        if (!contract.employerSigned() || !contract.freelancerSigned()) {
            throw new BusinessException(ErrorCode.CONTRACT_NOT_ACTIVATABLE);
        }
        if (!"IN_PROGRESS".equals(contract.status())) {
            throw new BusinessException(ErrorCode.CONTRACT_NOT_IN_PROGRESS);
        }
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

    /**
     * DataIntegrityViolationException이 유니크 제약 위반인지 확인.
     * PostgreSQL(23505) 및 MariaDB(1062)를 모두 지원합니다.
     * FK·NOT NULL 등 다른 무결성 위반과 구분하여 동시 결제 감지에만 사용.
     */
    private static boolean isUniqueConstraintViolation(DataIntegrityViolationException ex) {
        Throwable cause = ex.getCause();
        while (cause != null) {
            if (cause instanceof java.sql.SQLException sqlException) {
                String sqlState = sqlException.getSQLState();
                int errorCode = sqlException.getErrorCode();
                // PostgreSQL: 23505, MariaDB/MySQL: 1062
                if ("23505".equals(sqlState) || "23000".equals(sqlState) || errorCode == 1062) {
                    return true;
                }
            }
            if (cause instanceof org.hibernate.exception.ConstraintViolationException cve) {
                String sqlState = cve.getSQLState();
                int errorCode = cve.getErrorCode();
                if ("23505".equals(sqlState) || "23000".equals(sqlState) || errorCode == 1062) {
                    return true;
                }
            }
            // 메시지 기반 보조 체크
            String message = cause.getMessage();
            if (message != null && message.toLowerCase(java.util.Locale.ROOT).contains("duplicate")) {
                return true;
            }
            cause = cause.getCause();
        }
        return false;
    }

    /**
     * 정산 레코드 동시 생성 감지 시 발생시키는 내부 센티넬 예외.
     * {@code verifyContractPayment}의 inner DIVE catch에서만 생성되며,
     * outer catch의 전용 핸들러가 이 예외를 포착하여 포트원 보상 취소를 건너뜁니다.
     * (이미 다른 요청이 결제를 정상 처리했으므로 취소하면 안 됩니다.)
     */
    private static class ConcurrentSettlementException extends RuntimeException {
        ConcurrentSettlementException(DataIntegrityViolationException cause) {
            super("Concurrent settlement creation detected — payment was already processed", cause);
        }
    }
}
