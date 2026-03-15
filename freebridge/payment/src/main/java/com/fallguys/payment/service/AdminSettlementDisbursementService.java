package com.fallguys.payment.service;

import com.fallguys.common.exception.BusinessException;
import com.fallguys.common.exception.ErrorCode;
import com.fallguys.payment.entity.*;
import com.fallguys.payment.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminSettlementDisbursementService {

        private final EmployerSettlementRepository employerSettlementRepository;
        private final WalletRepository walletRepository;
        private final WalletTransactionRepository walletTransactionRepository;
        private final FreelancerSettlementRepository freelancerSettlementRepository;

        @Transactional(propagation = Propagation.REQUIRES_NEW)
        public void processSingleDisbursement(Long fsId) {
                // [Atomicity] 비관적 락을 사용하여 FreelancerSettlement를 최신 상태로 조회
                FreelancerSettlement fs = freelancerSettlementRepository.findByIdWithLock(fsId)
                                .orElseThrow(() -> new BusinessException(ErrorCode.SETTLEMENT_NOT_FOUND));

                // Guard: skip if already processed by a concurrent run
                if (fs.getStatus() != FreelancerSettlementStatus.PENDING) {
                        log.warn("정산 이미 처리됨, 건너뜀: freelancerSettlementId={}, status={}",
                                        fs.getId(), fs.getStatus());
                        return;
                }

                // [Atomicity] 비관적 락을 사용하여 EmployerSettlement를 최신 상태로 조회
                EmployerSettlement es = employerSettlementRepository.findByIdWithLock(fs.getEmployerSettlementId())
                                .orElseThrow(() -> new BusinessException(ErrorCode.SETTLEMENT_NOT_FOUND));

                // PLATFORM_ESCROW 에서 차감 (billingAmount + platformFee = totalPayment)
                // 지갑을 비관적 락을 걸어서 다시 조회하여 동시성 이슈를 방지합니다.
                Wallet escrowWalletLocked = walletRepository.findByWalletTypeWithLock(WalletType.PLATFORM_ESCROW)
                                .orElseThrow(() -> new BusinessException(ErrorCode.WALLET_NOT_FOUND));
                if (escrowWalletLocked.getBalance() < es.getTotalPayment()) {
                        throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
                }
                escrowWalletLocked.debit(es.getTotalPayment());
                walletRepository.save(escrowWalletLocked);

                // PLATFORM_REVENUE 에 플랫폼 수수료(고용주 측) + 플랫폼 수수료(프리랜서 측) + 세금(프리랜서 측) 크레딧
                Wallet revenueWalletLocked = walletRepository.findByWalletTypeWithLock(WalletType.PLATFORM_REVENUE)
                                .orElseGet(() -> {
                                        Wallet w = new Wallet();
                                        w.setWalletType(WalletType.PLATFORM_REVENUE);
                                        w.setBalance(0L);
                                        try {
                                                return walletRepository.save(w);
                                        } catch (DataIntegrityViolationException e) {
                                                return walletRepository
                                                                .findByWalletTypeWithLock(WalletType.PLATFORM_REVENUE)
                                                                .orElseThrow(() -> new BusinessException(
                                                                                ErrorCode.WALLET_NOT_FOUND));
                                        }
                                });
                // revenueAmount = 고용주 측 수수료 + 프리랜서 측 수수료 + 세금
                // escrowDebit(es.totalPayment) = revenueAmount + fs.netAmount 가 성립해야 합니다.
                long revenueAmount = es.getPlatformFee() + fs.getPlatformFee() + fs.getTax();
                revenueWalletLocked.credit(revenueAmount);
                walletRepository.save(revenueWalletLocked);

                // 프리랜서 지갑 크레딧
                Wallet freelancerWallet = walletRepository
                                .findByOwnerIdAndWalletTypeWithLock(fs.getFreelancerId(), WalletType.FREELANCER)
                                .orElseGet(() -> {
                                        Wallet w = new Wallet();
                                        w.setOwnerId(fs.getFreelancerId());
                                        w.setWalletType(WalletType.FREELANCER);
                                        w.setBalance(0L);
                                        try {
                                                return walletRepository.save(w);
                                        } catch (DataIntegrityViolationException e) {
                                                return walletRepository.findByOwnerIdAndWalletTypeWithLock(
                                                                fs.getFreelancerId(), WalletType.FREELANCER)
                                                                .orElseThrow(() -> new BusinessException(
                                                                                ErrorCode.WALLET_NOT_FOUND));
                                        }
                                });
                freelancerWallet.credit(fs.getNetAmount());
                walletRepository.save(freelancerWallet);

                // WalletTransaction 기록
                walletTransactionRepository.save(new WalletTransaction(
                                escrowWalletLocked.getId(), TransactionType.DEBIT, es.getTotalPayment(),
                                TransactionReferenceType.FREELANCER_DISBURSEMENT, fs.getId(),
                                "정산 에스크로 출금 (회차 #" + fs.getInstallmentNumber() + ")", escrowWalletLocked.getBalance()));

                walletTransactionRepository.save(new WalletTransaction(
                                revenueWalletLocked.getId(), TransactionType.CREDIT, revenueAmount,
                                TransactionReferenceType.PLATFORM_FEE, fs.getId(),
                                "플랫폼 수수료 수익 (회차 #" + fs.getInstallmentNumber() + ")",
                                revenueWalletLocked.getBalance()));

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

        }
}
