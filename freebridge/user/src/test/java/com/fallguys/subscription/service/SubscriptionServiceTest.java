package com.fallguys.subscription.service;

import com.fallguys.subscription.api.request.SubscriptionChangeRequest;
import com.fallguys.subscription.api.response.SubscriptionChangeResultResponse;
import com.fallguys.subscription.api.response.SubscriptionResponse;
import com.fallguys.subscription.api.shared.ExternalPaymentPort;
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

    @Mock
    private ExternalPaymentPort externalPaymentPort;

    @InjectMocks
    private SubscriptionServiceImpl subscriptionService;

    @Test
    @DisplayName("구독 조회: PRO 플랜이면 다음 결제일 포함 반환")
    void getSubscription_ProPlan_Success_WithBillingDate() {
        Long userId = 1L;
        LocalDateTime mockDate = LocalDateTime.of(2026, 4, 1, 9, 0);

        when(externalSubscriptionPort.getCurrentPlan(userId)).thenReturn(PlanGrade.PRO);
        when(externalPaymentPort.getNextBillingDate(userId)).thenReturn(mockDate);

        SubscriptionResponse result = subscriptionService.getSubscription(userId);

        assertThat(result.planGrade()).isEqualTo("PRO");
        assertThat(result.feeRate()).isEqualTo(10.0);
        assertThat(result.monthlyPrice()).isEqualTo(19900);
        assertThat(result.nextBillingDate()).isEqualTo(mockDate);

        verify(externalPaymentPort, times(1)).getNextBillingDate(userId);
    }

    @Test
    @DisplayName("구독 조회: userId null이면 예외")
    void getSubscription_NullUserId_ThrowsException() {
        assertThatThrownBy(() -> subscriptionService.getSubscription(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("구독 변경: 동일 플랜 변경 시 예외")
    void changePlan_SamePlan_ThrowsException() {
        Long userId = 1L;
        SubscriptionChangeRequest request = new SubscriptionChangeRequest("PRO", "billing-key");
        when(externalSubscriptionPort.getCurrentPlan(userId)).thenReturn(PlanGrade.PRO);

        assertThatThrownBy(() -> subscriptionService.changePlan(userId, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("동일한 플랜");
    }

    @Test
    @DisplayName("구독 변경: 유효하지 않은 플랜명 예외")
    void changePlan_InvalidPlanName_ThrowsException() {
        Long userId = 1L;
        SubscriptionChangeRequest request = new SubscriptionChangeRequest("GOLD", null);

        assertThatThrownBy(() -> subscriptionService.changePlan(userId, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("유효하지 않은 플랜 값");
    }

    @Test
    @DisplayName("업그레이드: BASIC->PRO 결제 성공 시 즉시 반영")
    void changePlan_Upgrade_BasicToPro_PaymentSuccess() {
        Long userId = 1L;
        LocalDateTime nextBillingDate = LocalDateTime.of(2026, 4, 1, 9, 0);
        SubscriptionChangeRequest request = new SubscriptionChangeRequest("PRO", "billing-key-123");

        when(externalSubscriptionPort.getCurrentPlan(userId)).thenReturn(PlanGrade.BASIC);
        when(externalPaymentPort.requestSubscriptionPayment(userId, "PRO", 19900, "billing-key-123"))
                .thenReturn(new ExternalPaymentPort.PaymentResult(true, 999L, null, null));
        when(externalPaymentPort.getNextBillingDate(userId)).thenReturn(nextBillingDate);

        SubscriptionChangeResultResponse result = subscriptionService.changePlan(userId, request);

        assertThat(result.currentPlanGrade()).isEqualTo("PRO");
        assertThat(result.pendingPlanGrade()).isNull();
        assertThat(result.status()).isEqualTo("ACTIVE");
        assertThat(result.nextBillingDate()).isEqualTo(nextBillingDate);

        verify(externalSubscriptionPort).changePlan(userId, PlanGrade.PRO);
        verify(externalSubscriptionPort, never()).schedulePlanDowngrade(anyLong(), any(), any());
    }

    @Test
    @DisplayName("업그레이드: 결제 실패 시 플랜 변경 없음")
    void changePlan_Upgrade_PaymentFailed_PlanNotChanged() {
        Long userId = 1L;
        SubscriptionChangeRequest request = new SubscriptionChangeRequest("PRO", "billing-key-bad");
        when(externalSubscriptionPort.getCurrentPlan(userId)).thenReturn(PlanGrade.BASIC);
        when(externalPaymentPort.requestSubscriptionPayment(userId, "PRO", 19900, "billing-key-bad"))
                .thenReturn(new ExternalPaymentPort.PaymentResult(false, null, "CARD_DECLINED", "card declined"));

        assertThatThrownBy(() -> subscriptionService.changePlan(userId, request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("구독 결제가 실패");

        verify(externalSubscriptionPort, never()).changePlan(anyLong(), any());
    }

    @Test
    @DisplayName("다운그레이드: PRIME->PRO는 결제 없이 다음 결제일 예약")
    void changePlan_Downgrade_PrimeToPro_ScheduledNoPayment() {
        Long userId = 1L;
        LocalDateTime mockDate = LocalDateTime.of(2026, 4, 1, 9, 0);
        SubscriptionChangeRequest request = new SubscriptionChangeRequest("PRO", null);

        when(externalSubscriptionPort.getCurrentPlan(userId)).thenReturn(PlanGrade.PRIME);
        when(externalPaymentPort.getNextBillingDate(userId)).thenReturn(mockDate);

        SubscriptionChangeResultResponse result = subscriptionService.changePlan(userId, request);

        assertThat(result.currentPlanGrade()).isEqualTo("PRIME");
        assertThat(result.pendingPlanGrade()).isEqualTo("PRO");
        assertThat(result.status()).isEqualTo("CHANGE_RESERVED");
        assertThat(result.nextBillingDate()).isEqualTo(mockDate);

        verify(externalSubscriptionPort).schedulePlanDowngrade(userId, PlanGrade.PRO, mockDate);
        verify(externalSubscriptionPort, never()).changePlan(anyLong(), any());
        verify(externalPaymentPort, never()).requestSubscriptionPayment(anyLong(), any(), anyLong(), any());
    }

    @Test
    @DisplayName("구독 취소: 유료 플랜은 BASIC 예약 전환")
    void cancelSubscription_ProPlan_ReservedToBasic() {
        Long userId = 1L;
        LocalDateTime mockDate = LocalDateTime.of(2026, 4, 1, 9, 0);
        when(externalSubscriptionPort.getCurrentPlan(userId)).thenReturn(PlanGrade.PRO);
        when(externalPaymentPort.getNextBillingDate(userId)).thenReturn(mockDate);

        SubscriptionChangeResultResponse result = subscriptionService.cancelSubscription(userId);

        assertThat(result.currentPlanGrade()).isEqualTo("PRO");
        assertThat(result.pendingPlanGrade()).isEqualTo("BASIC");
        assertThat(result.status()).isEqualTo("CANCEL_RESERVED");
        assertThat(result.nextBillingDate()).isEqualTo(mockDate);

        verify(externalSubscriptionPort, times(1)).cancelSubscription(userId, mockDate);
        verify(externalPaymentPort, times(1)).getNextBillingDate(userId);
    }

    @Test
    @DisplayName("구독 취소: 이미 BASIC이면 예외")
    void cancelSubscription_AlreadyBasicPlan_ThrowsException() {
        Long userId = 1L;
        when(externalSubscriptionPort.getCurrentPlan(userId)).thenReturn(PlanGrade.BASIC);

        assertThatThrownBy(() -> subscriptionService.cancelSubscription(userId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("BASIC");
    }
}