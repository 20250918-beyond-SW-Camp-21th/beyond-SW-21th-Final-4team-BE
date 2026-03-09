package com.fallguys.payment.service;

import com.fallguys.common.exception.BusinessException;
import com.fallguys.common.exception.ErrorCode;
import com.fallguys.payment.api.web.dto.*;
import com.fallguys.payment.entity.FreelancerSettlementStatus;
import com.fallguys.payment.entity.TransactionReferenceType;
import com.fallguys.payment.entity.Wallet;
import com.fallguys.payment.entity.WalletType;
import com.fallguys.payment.repository.FreelancerSettlementRepository;
import com.fallguys.payment.repository.WalletRepository;
import com.fallguys.payment.repository.WalletTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class WalletService {

    private final WalletRepository walletRepository;
    private final WalletTransactionRepository walletTransactionRepository;
    private final FreelancerSettlementRepository freelancerSettlementRepository;

    @Transactional(readOnly = true)
    public EmployerWalletSummaryResponse getEmployerSummary(Long employerId) {
        Wallet wallet = walletRepository.findByOwnerIdAndWalletType(employerId, WalletType.EMPLOYER)
                .orElse(null);
        if (wallet == null) {
            return new EmployerWalletSummaryResponse(0L, 0);
        }

        Long totalPaidOut = walletTransactionRepository.sumDebitByWalletId(wallet.getId());
        Integer transactionCount = walletTransactionRepository.countByWalletId(wallet.getId());

        return new EmployerWalletSummaryResponse(totalPaidOut, transactionCount);
    }

    @Transactional(readOnly = true)
    public PageResponse<WalletTransactionItem> getEmployerTransactions(
            Long employerId, String referenceType, int page, int size) {

        if (size <= 0) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }

        Wallet wallet = walletRepository.findByOwnerIdAndWalletType(employerId, WalletType.EMPLOYER)
                .orElse(null);
        if (wallet == null) {
            return new PageResponse<>(List.of(), 0L, 0, page);
        }

        Pageable pageable = PageRequest.of(Math.max(0, page - 1), size,
                Sort.by(Sort.Direction.DESC, "createdAt"));

        TransactionReferenceType parsedRefType = null;
        if (!"ALL".equalsIgnoreCase(referenceType)) {
            try {
                parsedRefType = TransactionReferenceType.valueOf(referenceType);
            } catch (IllegalArgumentException e) {
                throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
            }
        }

        final TransactionReferenceType finalRefType = parsedRefType;
        var pageResult = (finalRefType == null)
                ? walletTransactionRepository.findByWalletId(wallet.getId(), pageable)
                : walletTransactionRepository.findByWalletIdAndReferenceType(
                        wallet.getId(), finalRefType, pageable);

        List<WalletTransactionItem> items = pageResult.getContent().stream()
                .map(t -> new WalletTransactionItem(
                        t.getId(), t.getType().name(), t.getAmount(),
                        t.getReferenceType().name(), t.getReferenceId(),
                        t.getDescription(), t.getBalanceAfter(), t.getCreatedAt()))
                .toList();

        return new PageResponse<>(items, pageResult.getTotalElements(),
                pageResult.getTotalPages(), page);
    }

    @Transactional(readOnly = true)
    public FreelancerWalletSummaryResponse getFreelancerSummary(Long freelancerId) {
        Wallet wallet = walletRepository.findByOwnerIdAndWalletType(freelancerId, WalletType.FREELANCER)
                .orElse(null);

        // PENDING 상태 정산의 예정 수령액 (지갑 미생성 상태에서도 조회 가능)
        Long pendingAmount = freelancerSettlementRepository
                .sumNetAmountByFreelancerIdAndStatusPending(freelancerId);

        if (wallet == null) {
            return new FreelancerWalletSummaryResponse(0L, pendingAmount, 0);
        }

        Long totalEarned = wallet.getBalance();
        Integer transactionCount = walletTransactionRepository.countByWalletId(wallet.getId());

        return new FreelancerWalletSummaryResponse(totalEarned, pendingAmount, transactionCount);
    }

    @Transactional(readOnly = true)
    public PageResponse<WalletTransactionItem> getFreelancerTransactions(Long freelancerId, int page, int size) {
        if (size <= 0) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
        Wallet wallet = walletRepository.findByOwnerIdAndWalletType(freelancerId, WalletType.FREELANCER)
                .orElse(null);
        if (wallet == null) {
            return new PageResponse<>(List.of(), 0L, 0, page);
        }

        Pageable pageable = PageRequest.of(Math.max(0, page - 1), size,
                Sort.by(Sort.Direction.DESC, "createdAt"));

        var pageResult = walletTransactionRepository.findByWalletId(wallet.getId(), pageable);

        List<WalletTransactionItem> items = pageResult.getContent().stream()
                .map(t -> new WalletTransactionItem(
                        t.getId(), t.getType().name(), t.getAmount(),
                        t.getReferenceType().name(), t.getReferenceId(),
                        t.getDescription(), t.getBalanceAfter(), t.getCreatedAt()))
                .toList();

        return new PageResponse<>(items, pageResult.getTotalElements(),
                pageResult.getTotalPages(), page);
    }

    @Transactional(readOnly = true)
    public PlatformWalletResponse getEscrowWallet() {
        Wallet wallet = walletRepository.findByWalletType(WalletType.PLATFORM_ESCROW)
                .orElse(buildEmptyWallet(WalletType.PLATFORM_ESCROW));
        return new PlatformWalletResponse(wallet.getWalletType().name(), wallet.getBalance(), wallet.getUpdatedAt());
    }

    @Transactional(readOnly = true)
    public PlatformWalletResponse getRevenueWallet() {
        Wallet wallet = walletRepository.findByWalletType(WalletType.PLATFORM_REVENUE)
                .orElse(buildEmptyWallet(WalletType.PLATFORM_REVENUE));
        return new PlatformWalletResponse(wallet.getWalletType().name(), wallet.getBalance(), wallet.getUpdatedAt());
    }

    private Wallet buildEmptyWallet(WalletType type) {
        Wallet w = new Wallet();
        w.setWalletType(type);
        w.setBalance(0L);
        return w;
    }
}