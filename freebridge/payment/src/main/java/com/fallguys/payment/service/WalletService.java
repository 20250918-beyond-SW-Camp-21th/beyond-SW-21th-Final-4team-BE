package com.fallguys.payment.service;

import com.fallguys.payment.api.web.dto.*;

public interface WalletService {

    EmployerWalletSummaryResponse getEmployerSummary(Long employerId);

    PageResponse<WalletTransactionItem> getEmployerTransactions(
            Long employerId, String referenceType, int page, int size);

    FreelancerWalletSummaryResponse getFreelancerSummary(Long freelancerId);

    PageResponse<WalletTransactionItem> getFreelancerTransactions(Long freelancerId, int page, int size);

    PlatformWalletResponse getEscrowWallet();

    PlatformWalletResponse getRevenueWallet();
}
