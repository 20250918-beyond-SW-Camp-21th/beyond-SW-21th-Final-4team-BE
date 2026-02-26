package com.fallguys.payment.service;

import com.fallguys.payment.api.web.dto.*;

public interface AdminSettlementService {

    void generateSettlements(Long contractId);

    void runDisbursement();

    PageResponse<EmployerSettlementItem> listAllSettlements(String status, int page, int size);
}
