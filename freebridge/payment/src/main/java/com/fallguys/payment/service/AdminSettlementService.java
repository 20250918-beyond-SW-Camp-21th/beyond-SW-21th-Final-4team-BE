package com.fallguys.payment.service;

import com.fallguys.common.exception.BusinessException;
import com.fallguys.common.exception.ErrorCode;
import com.fallguys.common.api.contract.ContractInfo;
import com.fallguys.common.api.contract.ContractQuery;
import com.fallguys.payment.api.web.dto.*;
import com.fallguys.payment.api.web.dto.CancellationResult;
import com.fallguys.payment.entity.*;
import com.fallguys.payment.portone.PortOneApiClient;
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
        private final WalletTransactionRepository walletTransactionRepository;
        private final ContractQuery contractQuery;
        private final EmployerSettlementService employerSettlementService;
        private final PortOneApiClient portOneApiClient;

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
         * PLATFORM_ESCROW → PLATFORM_REVENUE + FREELANCER 지갑으로 이체 (DB 처리)
         */
        @Transactional
        public void runDisbursement() {
                LocalDate today = LocalDate.now();
                List<FreelancerSettlement> pendingList = freelancerSettlementRepository
                                .findByStatusAndScheduledDateLessThanEqualWithLock(FreelancerSettlementStatus.PENDING,
                                                today);

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

        private void processSingleDisbursement(FreelancerSettlement fs, Wallet escrowWallet, Wallet revenueWallet) {
                // Guard: skip if already processed by a concurrent run
                if (fs.getStatus() != FreelancerSettlementStatus.PENDING) {
                        log.warn("정산 이미 처리됨, 건너뜀: freelancerSettlementId={}, status={}",
                                        fs.getId(), fs.getStatus());
                        return;
                }

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

        /**
         * 계약 취소 — 에스크로에 묶인 PAID 회차만 환불 처리
         * DISBURSED 회차는 이미 프리랜서에게 지급됐으므로 취소 대상에서 제외
         */
        @Transactional
        public CancellationResult cancelContractSettlements(Long contractId) {
                List<EmployerSettlement> toCancel = employerSettlementRepository
                                .findByContractId(contractId).stream()
                                .filter(e -> e.getStatus() == EmployerSettlementStatus.PAID)
                                .toList();

                if (toCancel.isEmpty()) {
                        log.info("취소할 정산 레코드 없음 (이미 지급 완료 또는 레코드 없음): contractId={}", contractId);
                        return new CancellationResult(contractId, 0, 0L);
                }

                // 모든 PAID 회차는 동일한 transactionId(PortOne paymentId)를 공유
                String paymentId = toCancel.get(0).getTransactionId();
                Long employerId = toCancel.get(0).getEmployerId();

                Wallet escrowWallet = walletRepository.findByWalletType(WalletType.PLATFORM_ESCROW)
                                .orElseThrow(() -> new BusinessException(ErrorCode.WALLET_NOT_FOUND));

                long refundTotal = 0L;
                for (EmployerSettlement es : toCancel) {
                        FreelancerSettlement fs = freelancerSettlementRepository
                                        .findByEmployerSettlementId(es.getId())
                                        .orElseThrow(() -> new BusinessException(ErrorCode.SETTLEMENT_NOT_FOUND));

                        escrowWallet.debit(es.getTotalPayment());
                        walletTransactionRepository.save(new WalletTransaction(
                                        escrowWallet.getId(), TransactionType.DEBIT, es.getTotalPayment(),
                                        TransactionReferenceType.REFUND, es.getId(),
                                        "계약 취소 에스크로 출금 (회차 #" + es.getInstallmentNumber() + ")",
                                        escrowWallet.getBalance()));

                        es.cancel();
                        fs.cancel();
                        refundTotal += es.getTotalPayment();
                }
                walletRepository.save(escrowWallet);

                // PortOne 실제 환불 호출 (고용주의 결제 수단으로 환불)
                portOneApiClient.cancelPayment(paymentId, refundTotal, "관리자 계약 취소 환불");

                // 고용주 지갑 credit + 트랜잭션 기록
                Wallet employerWallet = employerSettlementService.getOrCreateUserWallet(employerId,
                                WalletType.EMPLOYER);
                employerWallet.credit(refundTotal);
                walletRepository.save(employerWallet);
                walletTransactionRepository.save(new WalletTransaction(
                                employerWallet.getId(), TransactionType.CREDIT, refundTotal,
                                TransactionReferenceType.REFUND, contractId,
                                "계약 취소 환불 입금 (계약 #" + contractId + ")",
                                employerWallet.getBalance()));

                log.info("계약 정산 취소 완료: contractId={}, cancelledInstallments={}, refundedAmount={}",
                                contractId, toCancel.size(), refundTotal);
                return new CancellationResult(contractId, toCancel.size(), refundTotal);
        }

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