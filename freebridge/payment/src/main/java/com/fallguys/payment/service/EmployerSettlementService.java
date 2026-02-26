package com.fallguys.payment.service;

import com.fallguys.payment.api.web.dto.*;

public interface EmployerSettlementService {

    PageResponse<EmployerSettlementItem> listSettlements(
            Long employerId, String status, String dateRange,
            String search, String sort, int page, int size);

    EmployerSettlementSummaryResponse getSummary(Long employerId);

    EmployerSettlementNextResponse getNextSettlement(Long employerId);

    EmployerSettlementDetailResponse getSettlementDetail(Long employerId, Long settlementId);

    String getInvoicePdfUrl(Long employerId, Long settlementId);

    VerifyPaymentResponse verifyContractPayment(String impUid, Long contractId, Long employerId);
}
