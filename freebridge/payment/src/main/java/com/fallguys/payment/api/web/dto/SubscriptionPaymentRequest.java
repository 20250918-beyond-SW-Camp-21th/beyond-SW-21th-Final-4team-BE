package com.fallguys.payment.api.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class SubscriptionPaymentRequest {

    private Long employerId;
    private String planType;
    private Long amount;

    @JsonProperty("imp_uid")
    private String impUid;
}
