package com.fallguys.payment.service.impl;

import com.fallguys.payment.api.web.dto.*;
import com.fallguys.payment.service.SubscriptionPaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SubscriptionPaymentServiceImpl implements SubscriptionPaymentService {

    @Override
    public SubscriptionPaymentResponse processPayment(SubscriptionPaymentRequest request) {
        // TODO: implement
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public SubscriptionBillingItem getBillingById(Long billingId) {
        // TODO: implement
        throw new UnsupportedOperationException("Not implemented yet");
    }
}
