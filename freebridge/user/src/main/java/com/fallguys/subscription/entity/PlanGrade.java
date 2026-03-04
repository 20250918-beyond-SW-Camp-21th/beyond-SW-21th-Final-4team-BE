package com.fallguys.subscription.entity;

/**
 * 구독 플랜 등급을 정의하는 Enum입니다.
 * Employer에게만 적용됩니다.
 */
public enum PlanGrade {

    BASIC(0, 12.0),
    PRO(19900, 10.0),
    PRIME(39900, 7.0);

    private final int monthlyPrice;
    private final double feeRate;

    PlanGrade(int monthlyPrice, double feeRate) {
        this.monthlyPrice = monthlyPrice;
        this.feeRate = feeRate;
    }

    public int getMonthlyPrice() {
        return monthlyPrice;
    }

    public double getFeeRate() {
        return feeRate;
    }
}
