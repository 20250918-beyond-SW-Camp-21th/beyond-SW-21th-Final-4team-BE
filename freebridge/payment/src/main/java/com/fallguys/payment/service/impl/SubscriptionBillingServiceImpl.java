package com.fallguys.payment.service.impl;

import com.fallguys.payment.api.web.dto.*;
import com.fallguys.payment.service.SubscriptionBillingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SubscriptionBillingServiceImpl implements SubscriptionBillingService {

    @Override
    public PageResponse<SubscriptionBillingItem> getBillingHistory(Long employerId, String status, int page, int size) {
        // TODO: implement
        throw new UnsupportedOperationException("Not implemented yet");
    }
}
