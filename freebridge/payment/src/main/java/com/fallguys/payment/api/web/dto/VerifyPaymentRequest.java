package com.fallguys.payment.api.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class VerifyPaymentRequest {

    @NotBlank(message = "paymentId는 필수입니다.")
    private String paymentId;

    @NotNull(message = "contractId는 필수입니다.")
    private Long contractId;
}
