package com.fallguys.payment.api.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class VerifyPaymentRequest {

    @JsonProperty("imp_uid")
    private String impUid;

    private Long contractId;
}
