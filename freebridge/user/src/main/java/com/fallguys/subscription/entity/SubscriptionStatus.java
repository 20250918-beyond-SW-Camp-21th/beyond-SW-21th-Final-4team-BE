package com.fallguys.subscription.entity;

public enum SubscriptionStatus {
    /** 정상 구독 중 */
    ACTIVE,
    /** 취소 예약됨 (현재 기간 만료 후 BASIC 전환 예정) */
    CANCEL_RESERVED,
    /** 만료됨 */
    EXPIRED
}
