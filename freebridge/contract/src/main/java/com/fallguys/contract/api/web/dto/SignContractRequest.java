package com.fallguys.contract.api.web.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class SignContractRequest {

    private String signature;  // Base64 data URL
}