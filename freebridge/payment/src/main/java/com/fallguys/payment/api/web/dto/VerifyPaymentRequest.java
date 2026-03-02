package com.fallguys.payment.api.web.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class VerifyPaymentRequest {

    private String paymentId;
    private Long contractId;
}
