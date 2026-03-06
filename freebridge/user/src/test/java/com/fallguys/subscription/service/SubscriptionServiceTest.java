package com.fallguys.subscription.service;

import com.fallguys.common.exception.BusinessException;
import com.fallguys.common.exception.ErrorCode;
import com.fallguys.subscription.api.request.SubscriptionChangeRequest;
import com.fallguys.subscription.api.response.SubscriptionChangeResultResponse;
import com.fallguys.subscription.api.response.SubscriptionResponse;
import com.fallguys.subscription.api.shared.ExternalSubscriptionPort;
import com.fallguys.subscription.api.shared.ExternalPaymentPort;
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

    @Mock
    private ExternalPaymentPort externalPaymentPort;

    @InjectMocks
    private SubscriptionServiceImpl subscriptionService;

    @Test
    @DisplayName("êµ¬ë… ì¡°íšŒ: PROë¡?ë³€ê²½ì‹œ, nextBillingDate?€ ?”ê¸ˆ ?•ë³´ê°€ ?•ìƒ ë°˜í™˜?œë‹¤.")
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
    @DisplayName("êµ¬ë… ì¡°íšŒ: ë¹„ì–´?ˆëŠ” userID??ê²½ìš° Null Exception??ë°œìƒ?œí‚¨??")
    void getSubscription_NullUserId_ThrowsException() {
        assertThatThrownBy(() -> subscriptionService.getSubscription(null))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.SUBSCRIPTION_INVALID_REQUEST));
    }

    @Test
    @DisplayName("êµ¬ë… ë³€ê²? ?™ì¼???Œëžœ?¼ë¡œ ë³€ê²???BusinessException???¼ìœ¼?¨ë‹¤.")
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
    @DisplayName("êµ¬ë… ë³€ê²? ? íš¨?˜ì? ?Šì? ?Œëžœ?€ BusinessException???¼ìœ¼?¨ë‹¤.")
    void changePlan_InvalidPlanName_ThrowsException() {
        Long userId = 1L;
        SubscriptionChangeRequest request = new SubscriptionChangeRequest("GOLD", null);

        assertThatThrownBy(() -> subscriptionService.changePlan(userId, request))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.SUBSCRIPTION_INVALID_PLAN));
    }

    @Test
    @DisplayName("업그레이드: BASIC->PRO, 결제 요청 및 nextBillingDate 설정")
    void changePlan_Upgrade_BasicToPro_PaymentAndSchedule() {
        Long userId = 1L;
        SubscriptionChangeRequest request = new SubscriptionChangeRequest("PRO", "billing-key-123");

        when(externalSubscriptionPort.getCurrentPlan(userId)).thenReturn(PlanGrade.BASIC);
        when(externalSubscriptionPort.getNextBillingDate(userId)).thenReturn(null);
        when(externalPaymentPort.requestSubscriptionPayment(anyLong(), any(), anyLong(), any()))
                .thenReturn(new ExternalPaymentPort.PaymentResult(true, 1L, null, null));

        SubscriptionChangeResultResponse result = subscriptionService.changePlan(userId, request);

        assertThat(result.currentPlanGrade()).isEqualTo("PRO");
        assertThat(result.pendingPlanGrade()).isNull();
        assertThat(result.status()).isEqualTo("ACTIVE");
        assertThat(result.nextBillingDate()).isNotNull();

        verify(externalPaymentPort).requestSubscriptionPayment(anyLong(), any(), anyLong(), any());
        verify(externalSubscriptionPort).changePlan(userId, PlanGrade.PRO);
        verify(externalSubscriptionPort).saveBillingKey(userId, "billing-key-123");
        verify(externalSubscriptionPort).setNextBillingDate(anyLong(), any(LocalDateTime.class));
        verify(externalSubscriptionPort, never()).schedulePlanDowngrade(anyLong(), any(), any());
    }

    @Test
    @DisplayName("êµ¬ë… ë³€ê²?: ?¤ìš´ê·¸ë ˆ?´ë“œ reserves change for nextBillingDate")
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
    @DisplayName("êµ¬ë… ì·¨ì†Œ: reserves BASIC on nextBillingDate")
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
    @DisplayName("êµ¬ë… ì·¨ì†Œ: already BASIC throws BusinessException")
    void cancelSubscription_AlreadyBasicPlan_ThrowsException() {
        Long userId = 1L;
        when(externalSubscriptionPort.getCurrentPlan(userId)).thenReturn(PlanGrade.BASIC);

        assertThatThrownBy(() -> subscriptionService.cancelSubscription(userId))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.SUBSCRIPTION_ALREADY_BASIC));
    }
}
