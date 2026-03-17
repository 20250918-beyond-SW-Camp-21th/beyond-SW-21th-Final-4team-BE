package com.fallguys.payment.api.web.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionPaymentRequest {

    private Long employerId;
    private String planType;
    private Long amount;
    private String billingKey;
    private String paymentId;
}
