package com.fallguys.subscription.service;

import com.fallguys.common.exception.BusinessException;
import com.fallguys.common.exception.ErrorCode;
import com.fallguys.subscription.api.request.SubscriptionChangeRequest;
import com.fallguys.subscription.api.response.SubscriptionChangeResultResponse;
import com.fallguys.subscription.api.response.SubscriptionResponse;
import com.fallguys.subscription.api.shared.ExternalSubscriptionPort;
import com.fallguys.subscription.entity.PlanGrade;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SubscriptionServiceTest {

    @Mock
    private ExternalSubscriptionPort externalSubscriptionPort;

    @InjectMocks
    private SubscriptionServiceImpl subscriptionService;

    @Test
    @DisplayName("구독 조회: PRO로 변경시, nextBillingDate에 -PRO-가 나와야한다.")
    void getSubscription_ProPlan_Success_WithBillingDate() {
        Long userId = 1L;
        LocalDateTime mockDate = LocalDateTime.of(2026, 4, 1, 9, 0);

        when(externalSubscriptionPort.getCurrentPlan(userId)).thenReturn(PlanGrade.PRO);
        when(externalSubscriptionPort.getNextBillingDate(userId)).thenReturn(mockDate);

        SubscriptionResponse result = subscriptionService.getSubscription(userId);

        assertThat(result.planGrade()).isEqualTo("PRO");
        assertThat(result.feeRate()).isEqualTo(10.0);
        assertThat(result.monthlyPrice()).isEqualTo(19900);
        assertThat(result.nextBillingDate()).isEqualTo(mockDate);

        verify(externalSubscriptionPort, times(1)).getNextBillingDate(userId);
    }

    @Test
    @DisplayName("구독 조회: 비어있는 userID의 경우 Null Exception을 발생시킨다.")
    void getSubscription_NullUserId_ThrowsException() {
        assertThatThrownBy(() -> subscriptionService.getSubscription(null))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.SUBSCRIPTION_INVALID_REQUEST));
    }

    @Test
    @DisplayName("구독 변경: 동일한 플랜으로 변경 시 BusinessException을 일으킨다.")
    void changePlan_SamePlan_ThrowsException() {
        Long userId = 1L;
        SubscriptionChangeRequest request = new SubscriptionChangeRequest("PRO", "billing-key");
        when(externalSubscriptionPort.getCurrentPlan(userId)).thenReturn(PlanGrade.PRO);

        assertThatThrownBy(() -> subscriptionService.changePlan(userId, request))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.SUBSCRIPTION_SAME_PLAN));
    }

    @Test
    @DisplayName("구독 변경: 유효하지 않은 플랜은 BusinessException을 일으킨다.")
    void changePlan_InvalidPlanName_ThrowsException() {
        Long userId = 1L;
        SubscriptionChangeRequest request = new SubscriptionChangeRequest("GOLD", null);

        assertThatThrownBy(() -> subscriptionService.changePlan(userId, request))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.SUBSCRIPTION_INVALID_PLAN));
    }

    @Test
    @DisplayName("구독 변경: BASIC->PRO 플랜 업그레이드, billingKey, nextBillingDate")
    void changePlan_Upgrade_BasicToPro_NoPaymentCall() {
        Long userId = 1L;
        SubscriptionChangeRequest request = new SubscriptionChangeRequest("PRO", "billing-key-123");

        when(externalSubscriptionPort.getCurrentPlan(userId)).thenReturn(PlanGrade.BASIC);
        when(externalSubscriptionPort.getNextBillingDate(userId)).thenReturn(null);

        SubscriptionChangeResultResponse result = subscriptionService.changePlan(userId, request);

        assertThat(result.currentPlanGrade()).isEqualTo("PRO");
        assertThat(result.pendingPlanGrade()).isNull();
        assertThat(result.status()).isEqualTo("ACTIVE");
        assertThat(result.nextBillingDate()).isNotNull();

        verify(externalSubscriptionPort).changePlan(userId, PlanGrade.PRO);
        verify(externalSubscriptionPort).saveBillingKey(userId, "billing-key-123");
        verify(externalSubscriptionPort).setNextBillingDate(anyLong(), any(LocalDateTime.class));
        verify(externalSubscriptionPort, never()).schedulePlanDowngrade(anyLong(), any(), any());
    }

    @Test
    @DisplayName("구독 변경 : 다운그레이드 reserves change for nextBillingDate")
    void changePlan_Downgrade_PrimeToPro_ScheduledNoPayment() {
        Long userId = 1L;
        LocalDateTime mockDate = LocalDateTime.of(2026, 4, 1, 9, 0);
        SubscriptionChangeRequest request = new SubscriptionChangeRequest("PRO", null);

        when(externalSubscriptionPort.getCurrentPlan(userId)).thenReturn(PlanGrade.PRIME);
        when(externalSubscriptionPort.getNextBillingDate(userId)).thenReturn(mockDate);

        SubscriptionChangeResultResponse result = subscriptionService.changePlan(userId, request);

        assertThat(result.currentPlanGrade()).isEqualTo("PRIME");
        assertThat(result.pendingPlanGrade()).isEqualTo("PRO");
        assertThat(result.status()).isEqualTo("CHANGE_RESERVED");
        assertThat(result.nextBillingDate()).isEqualTo(mockDate);

        verify(externalSubscriptionPort).schedulePlanDowngrade(userId, PlanGrade.PRO, mockDate);
    }

    @Test
    @DisplayName("구독 취소: reserves BASIC on nextBillingDate")
    void cancelSubscription_ProPlan_ReservedToBasic() {
        Long userId = 1L;
        LocalDateTime mockDate = LocalDateTime.of(2026, 4, 1, 9, 0);
        when(externalSubscriptionPort.getCurrentPlan(userId)).thenReturn(PlanGrade.PRO);
        when(externalSubscriptionPort.getNextBillingDate(userId)).thenReturn(mockDate);

        SubscriptionChangeResultResponse result = subscriptionService.cancelSubscription(userId);

        assertThat(result.currentPlanGrade()).isEqualTo("PRO");
        assertThat(result.pendingPlanGrade()).isEqualTo("BASIC");
        assertThat(result.status()).isEqualTo("CANCEL_RESERVED");
        assertThat(result.nextBillingDate()).isEqualTo(mockDate);

        verify(externalSubscriptionPort, times(1)).cancelSubscription(userId, mockDate);
    }

    @Test
    @DisplayName("구독 취소: already BASIC throws BusinessException")
    void cancelSubscription_AlreadyBasicPlan_ThrowsException() {
        Long userId = 1L;
        when(externalSubscriptionPort.getCurrentPlan(userId)).thenReturn(PlanGrade.BASIC);

        assertThatThrownBy(() -> subscriptionService.cancelSubscription(userId))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.SUBSCRIPTION_ALREADY_BASIC));
    }
}
