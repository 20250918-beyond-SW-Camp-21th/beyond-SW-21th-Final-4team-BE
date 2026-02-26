package com.fallguys.payment.service;

import com.fallguys.payment.api.web.dto.*;

public interface SubscriptionPaymentService {

    SubscriptionPaymentResponse processPayment(SubscriptionPaymentRequest request);

    SubscriptionBillingItem getBillingById(Long billingId);
}
