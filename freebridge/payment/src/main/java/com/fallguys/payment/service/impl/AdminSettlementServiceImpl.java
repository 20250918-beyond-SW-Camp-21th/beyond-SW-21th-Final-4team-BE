package com.fallguys.payment.service.impl;

import com.fallguys.payment.api.web.dto.*;
import com.fallguys.payment.service.AdminSettlementService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AdminSettlementServiceImpl implements AdminSettlementService {

    @Override
    public void generateSettlements(Long contractId) {
        // TODO: implement
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public void runDisbursement() {
        // TODO: implement
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public PageResponse<EmployerSettlementItem> listAllSettlements(String status, int page, int size) {
        // TODO: implement
        throw new UnsupportedOperationException("Not implemented yet");
    }
}
