package com.fallguys.contract.api.web.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class SignContractRequest {

    private String signature;  // Base64 data URL
}