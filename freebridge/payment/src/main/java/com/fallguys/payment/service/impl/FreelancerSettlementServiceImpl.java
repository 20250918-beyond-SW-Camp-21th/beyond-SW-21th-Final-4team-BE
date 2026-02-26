package com.fallguys.payment.service.impl;

import com.fallguys.payment.api.web.dto.*;
import com.fallguys.payment.service.FreelancerSettlementService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class FreelancerSettlementServiceImpl implements FreelancerSettlementService {

    @Override
    public PageResponse<FreelancerSettlementItem> listSettlements(
            Long freelancerId, String status, String dateRange,
            String search, String sort, int page, int size) {
        // TODO: implement
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public FreelancerSettlementSummaryResponse getSummary(Long freelancerId) {
        // TODO: implement
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public FreelancerSettlementDetailResponse getSettlementDetail(Long freelancerId, Long settlementId) {
        // TODO: implement
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public String getReceiptPdfUrl(Long freelancerId, Long settlementId) {
        // TODO: implement
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public TaxInvoiceResponse requestTaxInvoice(Long freelancerId, Long settlementId, TaxInvoiceRequest request) {
        // TODO: implement
        throw new UnsupportedOperationException("Not implemented yet");
    }
}
