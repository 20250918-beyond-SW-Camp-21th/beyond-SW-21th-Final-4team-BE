package com.fallguys.payment.service.impl;

import com.fallguys.payment.api.web.dto.*;
import com.fallguys.payment.service.EmployerSettlementService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmployerSettlementServiceImpl implements EmployerSettlementService {

    @Override
    public PageResponse<EmployerSettlementItem> listSettlements(
            Long employerId, String status, String dateRange,
            String search, String sort, int page, int size) {
        // TODO: implement
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public EmployerSettlementSummaryResponse getSummary(Long employerId) {
        // TODO: implement
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public EmployerSettlementNextResponse getNextSettlement(Long employerId) {
        // TODO: implement
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public EmployerSettlementDetailResponse getSettlementDetail(Long employerId, Long settlementId) {
        // TODO: implement
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public String getInvoicePdfUrl(Long employerId, Long settlementId) {
        // TODO: implement
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public VerifyPaymentResponse verifyContractPayment(String impUid, Long contractId, Long employerId) {
        // TODO: implement
        throw new UnsupportedOperationException("Not implemented yet");
    }
}
