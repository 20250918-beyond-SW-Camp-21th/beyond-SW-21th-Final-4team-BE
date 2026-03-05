package com.fallguys.subscription.service;

import com.fallguys.subscription.api.request.SubscriptionChangeRequest;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SubscriptionServiceTest {

    @Mock
    private ExternalSubscriptionPort externalSubscriptionPort;

    @Mock
    private ExternalPaymentPort externalPaymentPort;

    @InjectMocks
    private SubscriptionServiceImpl subscriptionService;

    /* ==================== getSubscription ==================== */

    @Test
    @DisplayName("구독 조회: PRO 플랜 사용 중인 고용주의 구독 정보를 정상 반환한다")
    void getSubscription_ProPlan_Success() {
        Long userId = 1L;
        when(externalSubscriptionPort.getCurrentPlan(userId)).thenReturn(PlanGrade.PRO);

        SubscriptionResponse result = subscriptionService.getSubscription(userId);

        assertThat(result.planGrade()).isEqualTo("PRO");
        assertThat(result.feeRate()).isEqualTo(10.0);
        assertThat(result.monthlyPrice()).isEqualTo(19900);
        verify(externalSubscriptionPort, times(1)).getCurrentPlan(userId);
    }

    @Test
    @DisplayName("구독 조회: userId가 null이면 예외가 발생한다")
    void getSubscription_NullUserId_ThrowsException() {
        assertThatThrownBy(() -> subscriptionService.getSubscription(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("유효하지 않은 사용자 ID");
    }

    @Test
    @DisplayName("구독 조회: userId가 0 이하면 예외가 발생한다")
    void getSubscription_InvalidUserId_ThrowsException() {
        assertThatThrownBy(() -> subscriptionService.getSubscription(0L))
                .isInstanceOf(IllegalArgumentException.class);
    }

    /* ==================== changePlan - 기본 검증 ==================== */

    @Test
    @DisplayName("구독 변경: 현재와 동일한 플랜으로 변경 시 예외가 발생한다")
    void changePlan_SamePlan_ThrowsException() {
        Long userId = 1L;
        SubscriptionChangeRequest request = new SubscriptionChangeRequest("PRO", "billing-key");
        when(externalSubscriptionPort.getCurrentPlan(userId)).thenReturn(PlanGrade.PRO);

        assertThatThrownBy(() -> subscriptionService.changePlan(userId, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("동일한 플랜");
    }

    @Test
    @DisplayName("구독 변경: 유효하지 않은 플랜 이름 입력 시 예외가 발생한다")
    void changePlan_InvalidPlanName_ThrowsException() {
        Long userId = 1L;
        SubscriptionChangeRequest request = new SubscriptionChangeRequest("GOLD", null);

        assertThatThrownBy(() -> subscriptionService.changePlan(userId, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("유효하지 않은 플랜 값");
    }

    @Test
    @DisplayName("구독 변경: request가 null이면 예외가 발생한다")
    void changePlan_NullRequest_ThrowsException() {
        assertThatThrownBy(() -> subscriptionService.changePlan(1L, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    /* ==================== changePlan - BASIC 전환 ==================== */

    @Test
    @DisplayName("구독 변경: PRIME -> BASIC 변경 시 결제 없이 바로 BASIC으로 전환된다")
    void changePlan_ToBasic_NoPayment() {
        Long userId = 1L;
        SubscriptionChangeRequest request = new SubscriptionChangeRequest("BASIC", null);
        when(externalSubscriptionPort.getCurrentPlan(userId)).thenReturn(PlanGrade.PRIME);

        subscriptionService.changePlan(userId, request);

        verify(externalSubscriptionPort).changePlan(userId, PlanGrade.BASIC);
        verify(externalPaymentPort, never()).requestSubscriptionPayment(any(), any(), anyLong(), any());
    }

    /* ==================== changePlan - 업그레이드 (즉시 결제) ==================== */

    @Test
    @DisplayName("업그레이드: BASIC -> PRO 변경 시 결제 성공하면 즉시 플랜이 변경된다")
    void changePlan_Upgrade_BasicToPro_PaymentSuccess() {
        Long userId = 1L;
        SubscriptionChangeRequest request = new SubscriptionChangeRequest("PRO", "billing-key-123");
        when(externalSubscriptionPort.getCurrentPlan(userId)).thenReturn(PlanGrade.BASIC);
        when(externalPaymentPort.requestSubscriptionPayment(userId, "PRO", 19900, "billing-key-123"))
                .thenReturn(new ExternalPaymentPort.PaymentResult(true, 999L, null, null));

        subscriptionService.changePlan(userId, request);

        verify(externalSubscriptionPort).changePlan(userId, PlanGrade.PRO);
        verify(externalSubscriptionPort, never()).schedulePlanDowngrade(any(), any());
    }

    @Test
    @DisplayName("업그레이드: PRO -> PRIME 변경 시 결제 성공하면 즉시 플랜이 변경된다")
    void changePlan_Upgrade_ProToPrime_PaymentSuccess() {
        Long userId = 1L;
        SubscriptionChangeRequest request = new SubscriptionChangeRequest("PRIME", "billing-key-456");
        when(externalSubscriptionPort.getCurrentPlan(userId)).thenReturn(PlanGrade.PRO);
        when(externalPaymentPort.requestSubscriptionPayment(userId, "PRIME", 39900, "billing-key-456"))
                .thenReturn(new ExternalPaymentPort.PaymentResult(true, 1000L, null, null));

        subscriptionService.changePlan(userId, request);

        verify(externalSubscriptionPort).changePlan(userId, PlanGrade.PRIME);
        verify(externalSubscriptionPort, never()).schedulePlanDowngrade(any(), any());
    }

    @Test
    @DisplayName("업그레이드: 결제 실패 시 플랜이 변경되지 않고 예외가 발생한다")
    void changePlan_Upgrade_PaymentFailed_PlanNotChanged() {
        Long userId = 1L;
        SubscriptionChangeRequest request = new SubscriptionChangeRequest("PRO", "billing-key-bad");
        when(externalSubscriptionPort.getCurrentPlan(userId)).thenReturn(PlanGrade.BASIC);
        when(externalPaymentPort.requestSubscriptionPayment(userId, "PRO", 19900, "billing-key-bad"))
                .thenReturn(new ExternalPaymentPort.PaymentResult(false, null, "CARD_DECLINED", "카드 한도 초과"));

        assertThatThrownBy(() -> subscriptionService.changePlan(userId, request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("구독 결제가 실패하였습니다");

        verify(externalSubscriptionPort, never()).changePlan(any(), any());
    }

    @Test
    @DisplayName("업그레이드: 유료 플랜 변경 시 billingKey 없으면 결제 없이 예외가 발생한다")
    void changePlan_Upgrade_NoBillingKey_ThrowsException() {
        Long userId = 1L;
        SubscriptionChangeRequest request = new SubscriptionChangeRequest("PRO", null);
        when(externalSubscriptionPort.getCurrentPlan(userId)).thenReturn(PlanGrade.BASIC);

        assertThatThrownBy(() -> subscriptionService.changePlan(userId, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("billingKey");

        verify(externalPaymentPort, never()).requestSubscriptionPayment(any(), any(), anyLong(), any());
        verify(externalSubscriptionPort, never()).changePlan(any(), any());
    }

    /* ==================== changePlan - 다운그레이드 예약 (PRIME→PRO 등) ==================== */

    @Test
    @DisplayName("다운그레이드: PRIME -> PRO 변경 시 결제 없이 다음 결제일로 변경이 예약된다")
    void changePlan_Downgrade_PrimeToPro_ScheduledNoPayment() {
        Long userId = 1L;
        SubscriptionChangeRequest request = new SubscriptionChangeRequest("PRO", null);
        when(externalSubscriptionPort.getCurrentPlan(userId)).thenReturn(PlanGrade.PRIME);

        subscriptionService.changePlan(userId, request);

        // 다운그레이드 예약 호출
        verify(externalSubscriptionPort).schedulePlanDowngrade(userId, PlanGrade.PRO);
        // 즉시 변경 없음
        verify(externalSubscriptionPort, never()).changePlan(any(), any());
        // 결제 없음
        verify(externalPaymentPort, never()).requestSubscriptionPayment(any(), any(), anyLong(), any());
    }

    @Test
    @DisplayName("다운그레이드: PRIME -> PRO 변경 시 billingKey 없어도 정상 예약된다 (결제 불필요)")
    void changePlan_Downgrade_NoBillingKey_ScheduledSuccessfully() {
        Long userId = 2L;
        SubscriptionChangeRequest request = new SubscriptionChangeRequest("PRO", null);
        when(externalSubscriptionPort.getCurrentPlan(userId)).thenReturn(PlanGrade.PRIME);

        subscriptionService.changePlan(userId, request);

        verify(externalSubscriptionPort).schedulePlanDowngrade(userId, PlanGrade.PRO);
    }

    /* ==================== cancelSubscription ==================== */

    @Test
    @DisplayName("구독 취소: PRO 플랜 사용 중인 고용주가 취소 요청 시 정상 처리된다")
    void cancelSubscription_ProPlan_Success() {
        Long userId = 1L;
        when(externalSubscriptionPort.getCurrentPlan(userId)).thenReturn(PlanGrade.PRO);

        subscriptionService.cancelSubscription(userId);

        verify(externalSubscriptionPort, times(1)).cancelSubscription(userId);
    }

    @Test
    @DisplayName("구독 취소: 이미 BASIC 플랜 사용 중이면 예외가 발생한다")
    void cancelSubscription_AlreadyBasicPlan_ThrowsException() {
        Long userId = 1L;
        when(externalSubscriptionPort.getCurrentPlan(userId)).thenReturn(PlanGrade.BASIC);

        assertThatThrownBy(() -> subscriptionService.cancelSubscription(userId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("BASIC");
    }

    @Test
    @DisplayName("구독 취소: 정상 처리된다")
    void cancelSubscription_Success() {
        Long userId = 1L;
        when(externalSubscriptionPort.getCurrentPlan(userId)).thenReturn(PlanGrade.PRIME);

        subscriptionService.cancelSubscription(userId);

        verify(externalSubscriptionPort, times(1)).cancelSubscription(userId);
    }
}
