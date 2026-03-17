package com.fallguys.subscription.service;

import com.fallguys.common.exception.BusinessException;
import com.fallguys.common.exception.ErrorCode;
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
    @DisplayName("구독 조회: PRO 플랜은 nextBillingDate를 함께 반환한다")
    void getSubscription_ProPlan_Success_WithBillingDate() {
        Long userId = 1L;
        LocalDateTime mockDate = LocalDateTime.of(2026, 4, 1, 9, 0);

        when(externalSubscriptionPort.getCurrentPlan(userId)).thenReturn(PlanGrade.PRO);
        when(externalSubscriptionPort.getNextBillingDate(userId)).thenReturn(mockDate);

        SubscriptionResponse result = subscriptionService.getSubscription(userId);

        assertThat(result.planGrade()).isEqualTo("PRO");
        assertThat(result.feeRate()).isEqualTo(10.0);
        assertThat(result.monthlyPrice()).isEqualTo(9900);
        assertThat(result.nextBillingDate()).isEqualTo(mockDate);

        verify(externalSubscriptionPort, times(1)).getNextBillingDate(userId);
    }

    @Test
    @DisplayName("구독 조회: userId가 null이면 BusinessException이 발생한다")
    void getSubscription_NullUserId_ThrowsException() {
        assertThatThrownBy(() -> subscriptionService.getSubscription(null))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.SUBSCRIPTION_INVALID_REQUEST));
    }

    @Test
    @DisplayName("플랜 변경: 동일한 플랜으로 요청하면 BusinessException이 발생한다")
    void changePlan_SamePlan_ThrowsException() {
        Long userId = 1L;
        SubscriptionChangeRequest request = new SubscriptionChangeRequest("PRO", "billing-key", null);
        when(externalSubscriptionPort.getCurrentPlan(userId)).thenReturn(PlanGrade.PRO);

        assertThatThrownBy(() -> subscriptionService.changePlan(userId, request))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.SUBSCRIPTION_SAME_PLAN));
    }

    @Test
    @DisplayName("플랜 변경: 유효하지 않은 플랜 값이면 BusinessException이 발생한다")
    void changePlan_InvalidPlanName_ThrowsException() {
        Long userId = 1L;
        SubscriptionChangeRequest request = new SubscriptionChangeRequest("GOLD", null, null);

        assertThatThrownBy(() -> subscriptionService.changePlan(userId, request))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.SUBSCRIPTION_INVALID_PLAN));
    }

    @Test
    @DisplayName("업그레이드: BASIC->PRO, 결제 요청 및 nextBillingDate 설정")
    void changePlan_Upgrade_BasicToPro_PaymentAndSchedule() {
        Long userId = 1L;
        SubscriptionChangeRequest request = new SubscriptionChangeRequest("PRO", null, "payment-123");

        when(externalSubscriptionPort.getCurrentPlan(userId)).thenReturn(PlanGrade.BASIC);
        when(externalSubscriptionPort.getNextBillingDate(userId)).thenReturn(null);
        when(externalPaymentPort.verifyOneTimeSubscriptionPayment(anyLong(), any(), anyLong(), any()))
                .thenReturn(new ExternalPaymentPort.PaymentResult(true, 1L, null, null));

        SubscriptionChangeResultResponse result = subscriptionService.changePlan(userId, request);

        assertThat(result.currentPlanGrade()).isEqualTo("PRO");
        assertThat(result.pendingPlanGrade()).isNull();
        assertThat(result.status()).isEqualTo("ACTIVE");
        assertThat(result.nextBillingDate()).isNull();

        verify(externalPaymentPort).verifyOneTimeSubscriptionPayment(anyLong(), any(), anyLong(), any());
        verify(externalSubscriptionPort).changePlan(userId, PlanGrade.PRO);
        verify(externalSubscriptionPort).setNextBillingDate(userId, null);
    }

    @Test
    @DisplayName("다운그레이드: PRIME->PRO, 결제 없이 즉시 반영하고 nextBillingDate 유지")
    void changePlan_Downgrade_PrimeToPro_ImmediateNoPayment() {
        Long userId = 1L;
        LocalDateTime mockDate = LocalDateTime.of(2026, 4, 1, 9, 0);
        SubscriptionChangeRequest request = new SubscriptionChangeRequest("PRO", null, null);

        when(externalSubscriptionPort.getCurrentPlan(userId)).thenReturn(PlanGrade.PRIME);
        when(externalSubscriptionPort.getNextBillingDate(userId)).thenReturn(mockDate);

        SubscriptionChangeResultResponse result = subscriptionService.changePlan(userId, request);

        assertThat(result.currentPlanGrade()).isEqualTo("PRO");
        assertThat(result.pendingPlanGrade()).isNull();
        assertThat(result.status()).isEqualTo("ACTIVE");
        assertThat(result.nextBillingDate()).isNull();

        verify(externalSubscriptionPort).changePlan(userId, PlanGrade.PRO);
        verify(externalSubscriptionPort).setNextBillingDate(userId, null);
        verify(externalPaymentPort, never()).verifyOneTimeSubscriptionPayment(anyLong(), any(), anyLong(), any());
    }

    @Test
    @DisplayName("다운그레이드: PRO->BASIC, changePlan으로 즉시 반영하고 nextBillingDate를 제거")
    void changePlan_Downgrade_ProToBasic_ImmediateAndClearBillingDate() {
        Long userId = 1L;
        SubscriptionChangeRequest request = new SubscriptionChangeRequest("BASIC", null, null);

        when(externalSubscriptionPort.getCurrentPlan(userId)).thenReturn(PlanGrade.PRO);
        when(externalSubscriptionPort.getNextBillingDate(userId)).thenReturn(LocalDateTime.of(2026, 4, 1, 9, 0));

        SubscriptionChangeResultResponse result = subscriptionService.changePlan(userId, request);

        assertThat(result.currentPlanGrade()).isEqualTo("BASIC");
        assertThat(result.pendingPlanGrade()).isNull();
        assertThat(result.status()).isEqualTo("ACTIVE");
        assertThat(result.nextBillingDate()).isNull();

        verify(externalSubscriptionPort).changePlan(userId, PlanGrade.BASIC);
        verify(externalSubscriptionPort).setNextBillingDate(userId, null);
        verify(externalPaymentPort, never()).verifyOneTimeSubscriptionPayment(anyLong(), any(), anyLong(), any());
    }
}
