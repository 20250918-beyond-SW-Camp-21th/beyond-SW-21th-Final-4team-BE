package com.fallguys.subscription.entity;


import lombok.Getter;

@Getter
public enum PlanGrade {

    BASIC(0, 12.0),
    PRO(9900, 10.0),
    PRIME(19900, 7.0);

    private final int monthlyPrice;
    private final double feeRate;

    PlanGrade(int monthlyPrice, double feeRate) {
        this.monthlyPrice = monthlyPrice;
        this.feeRate = feeRate;
    }

}
