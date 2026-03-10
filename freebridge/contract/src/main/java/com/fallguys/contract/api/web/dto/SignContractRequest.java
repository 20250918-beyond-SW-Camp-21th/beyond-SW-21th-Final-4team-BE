package com.fallguys.contract.api.web.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class SignContractRequest {

    private String signature;  // Base64 data URL

    // 프리랜서 서명 시 직접 입력 (FREELANCER 역할일 때만 필요)
    private String freelancerAddress;

    private String freelancerPhone;
}
