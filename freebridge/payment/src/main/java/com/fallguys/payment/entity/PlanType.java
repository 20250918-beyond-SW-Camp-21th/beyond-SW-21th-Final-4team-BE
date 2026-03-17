package com.fallguys.payment.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PlanType {
    FREE(0L),
    PRO(9900L),
    PRIME(19900L);

    /** 월 구독료 (KRW) */
    private final long monthlyPrice;
}
