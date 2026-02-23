package com.fallguys.common.ai.port;

import java.util.Map;

public interface ContractEngine {
    // 계약서 초안 생성 및 템플릿 관리
    String generateContract(Map<String, Object> agreementData);
}
