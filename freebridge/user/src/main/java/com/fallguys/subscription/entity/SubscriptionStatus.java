package com.fallguys.subscription.entity;

/**
 * 구독 상태를 정의하는 Enum입니다.
 */
public enum SubscriptionStatus {
    /** 정상 구독 중 */
    ACTIVE,
    /** 취소 예약됨 (현재 기간 만료 후 BASIC 전환 예정) */
    CANCEL_RESERVED,
    /** 만료됨 */
    EXPIRED
}
