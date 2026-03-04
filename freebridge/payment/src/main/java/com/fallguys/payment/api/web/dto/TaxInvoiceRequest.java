package com.fallguys.payment.api.web.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class TaxInvoiceRequest {

    private String businessRegistrationNumber;
    private String companyName;
    private String email;
}
