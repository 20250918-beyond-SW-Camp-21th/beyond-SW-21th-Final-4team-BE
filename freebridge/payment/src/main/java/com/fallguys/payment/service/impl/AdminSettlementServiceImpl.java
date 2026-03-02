package com.fallguys.payment.service.impl;

import com.fallguys.common.exception.BusinessException;
import com.fallguys.common.exception.ErrorCode;
import com.fallguys.contract.api.shared.ContractInfo;
import com.fallguys.contract.api.shared.ContractQuery;
import com.fallguys.payment.api.web.dto.*;
import com.fallguys.payment.entity.*;
import com.fallguys.payment.repository.*;
import com.fallguys.payment.service.AdminSettlementService;
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
public class AdminSettlementServiceImpl implements AdminSettlementService {

    private final EmployerSettlementRepository employerSettlementRepository;
    private final FreelancerSettlementRepository freelancerSettlementRepository;
    private final WalletRepository walletRepository;
    private final WalletTransactionRepository walletTransactionRepository;
    private final ContractQuery contractQuery;
    private final EmployerSettlementServiceImpl employerSettlementServiceImpl;

    /**
     * 계약 정산 레코드 수동 생성 (포트원 검증 없이, 어드민/테스트 용도)
     */
    @Override
    @Transactional
    public void generateSettlements(Long contractId) {
        ContractInfo contract = contractQuery.getContractInfo(contractId);

        List<EmployerSettlement> existing = employerSettlementRepository.findByContractId(contractId);
        if (!existing.isEmpty()) {
            log.warn("계약 #{} 에 대한 정산 레코드가 이미 존재합니다. 건너뜁니다.", contractId);
            return;
        }

        employerSettlementServiceImpl.createSettlementRecords(contract, "MANUAL-" + contractId, contract.employerId());
        log.info("계약 #{} 정산 레코드 수동 생성 완료", contractId);
    }

    /**
     * 프리랜서 월별 정산 실행
     * scheduledDate <= 오늘 이고 PENDING 상태인 FreelancerSettlement를 처리
     * PLATFORM_ESCROW → PLATFORM_REVENUE + FREELANCER 지갑으로 이체 (DB 처리)
     */
    @Override
    @Transactional
    public void runDisbursement() {
        LocalDate today = LocalDate.now();
        List<FreelancerSettlement> pendingList = freelancerSettlementRepository
                .findByStatusAndScheduledDateLessThanEqual(FreelancerSettlementStatus.PENDING, today);

        if (pendingList.isEmpty()) {
            log.info("지급 대상 정산 없음 (기준일: {})", today);
            return;
        }

        Wallet escrowWallet = walletRepository.findByWalletType(WalletType.PLATFORM_ESCROW)
                .orElseThrow(() -> new BusinessException(ErrorCode.WALLET_NOT_FOUND));
        Wallet revenueWallet = walletRepository.findByWalletType(WalletType.PLATFORM_REVENUE)
                .orElseGet(() -> {
                    Wallet w = new Wallet();
                    w.setWalletType(WalletType.PLATFORM_REVENUE);
                    w.setBalance(0L);
                    return walletRepository.save(w);
                });

        int successCount = 0;
        for (FreelancerSettlement fs : pendingList) {
            try {
                processSingleDisbursement(fs, escrowWallet, revenueWallet);
                successCount++;
            } catch (Exception e) {
                log.error("정산 처리 실패: freelancerSettlementId={}, error={}", fs.getId(), e.getMessage());
            }
        }

        log.info("정산 실행 완료: 성공={}/{}", successCount, pendingList.size());
    }

    private void processSingleDisbursement(FreelancerSettlement fs,
                                            Wallet escrowWallet, Wallet revenueWallet) {
        EmployerSettlement es = employerSettlementRepository.findById(fs.getEmployerSettlementId())
                .orElseThrow(() -> new BusinessException(ErrorCode.SETTLEMENT_NOT_FOUND));

        // PLATFORM_ESCROW 에서 차감 (billingAmount + platformFee = totalPayment)
        escrowWallet.debit(es.getTotalPayment());
        walletRepository.save(escrowWallet);

        // PLATFORM_REVENUE 에 플랫폼 수수료(고용주 측) + 세금(프리랜서 측) 크레딧
        long revenueAmount = es.getPlatformFee() + fs.getTax();
        revenueWallet.credit(revenueAmount);
        walletRepository.save(revenueWallet);

        // 프리랜서 지갑 크레딧
        Wallet freelancerWallet = walletRepository
                .findByOwnerIdAndWalletType(fs.getFreelancerId(), WalletType.FREELANCER)
                .orElseGet(() -> {
                    Wallet w = new Wallet();
                    w.setOwnerId(fs.getFreelancerId());
                    w.setWalletType(WalletType.FREELANCER);
                    w.setBalance(0L);
                    return walletRepository.save(w);
                });
        freelancerWallet.credit(fs.getNetAmount());
        walletRepository.save(freelancerWallet);

        // WalletTransaction 기록
        walletTransactionRepository.save(new WalletTransaction(
                escrowWallet.getId(), TransactionType.DEBIT, es.getTotalPayment(),
                TransactionReferenceType.FREELANCER_DISBURSEMENT, fs.getId(),
                "정산 에스크로 출금 (회차 #" + fs.getInstallmentNumber() + ")", escrowWallet.getBalance()));

        walletTransactionRepository.save(new WalletTransaction(
                revenueWallet.getId(), TransactionType.CREDIT, revenueAmount,
                TransactionReferenceType.PLATFORM_FEE, fs.getId(),
                "플랫폼 수수료 수익 (회차 #" + fs.getInstallmentNumber() + ")", revenueWallet.getBalance()));

        walletTransactionRepository.save(new WalletTransaction(
                freelancerWallet.getId(), TransactionType.CREDIT, fs.getNetAmount(),
                TransactionReferenceType.FREELANCER_DISBURSEMENT, fs.getId(),
                "프리랜서 정산 지급 (회차 #" + fs.getInstallmentNumber() + ")", freelancerWallet.getBalance()));

        // EmployerSettlement DISBURSED 처리
        es.markDisbursed();
        employerSettlementRepository.save(es);

        // FreelancerSettlement PAID 처리
        fs.markPaid();
        freelancerSettlementRepository.save(fs);

        log.debug("정산 지급 완료: freelancerId={}, installment={}, netAmount={}",
                fs.getFreelancerId(), fs.getInstallmentNumber(), fs.getNetAmount());
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<EmployerSettlementItem> listAllSettlements(String status, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(0, page - 1), size,
                Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<EmployerSettlement> pageResult;

        if (!"ALL".equalsIgnoreCase(status)) {
            EmployerSettlementStatus statusEnum = EmployerSettlementStatus.valueOf(status);
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
