package com.fallguys.payment.service.impl;

import com.fallguys.payment.api.web.dto.*;
import com.fallguys.payment.service.WalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class WalletServiceImpl implements WalletService {

    @Override
    public EmployerWalletSummaryResponse getEmployerSummary(Long employerId) {
        // TODO: implement
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public PageResponse<WalletTransactionItem> getEmployerTransactions(
            Long employerId, String referenceType, int page, int size) {
        // TODO: implement
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public FreelancerWalletSummaryResponse getFreelancerSummary(Long freelancerId) {
        // TODO: implement
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public PageResponse<WalletTransactionItem> getFreelancerTransactions(Long freelancerId, int page, int size) {
        // TODO: implement
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public PlatformWalletResponse getEscrowWallet() {
        // TODO: implement
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public PlatformWalletResponse getRevenueWallet() {
        // TODO: implement
        throw new UnsupportedOperationException("Not implemented yet");
    }
}
