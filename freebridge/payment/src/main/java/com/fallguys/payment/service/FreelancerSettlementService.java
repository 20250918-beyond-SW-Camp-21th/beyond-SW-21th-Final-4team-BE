package com.fallguys.payment.service;

import com.fallguys.payment.api.web.dto.*;

public interface FreelancerSettlementService {

    PageResponse<FreelancerSettlementItem> listSettlements(
            Long freelancerId, String status, String dateRange,
            String search, String sort, int page, int size);

    FreelancerSettlementSummaryResponse getSummary(Long freelancerId);

    FreelancerSettlementDetailResponse getSettlementDetail(Long freelancerId, Long settlementId);

    String getReceiptPdfUrl(Long freelancerId, Long settlementId);

    TaxInvoiceResponse requestTaxInvoice(Long freelancerId, Long settlementId, TaxInvoiceRequest request);
}
