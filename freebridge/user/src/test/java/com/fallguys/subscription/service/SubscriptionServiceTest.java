package com.fallguys.subscription.service;

import com.fallguys.subscription.api.request.SubscriptionCancelRequest;
import com.fallguys.subscription.api.request.SubscriptionChangeRequest;
import com.fallguys.subscription.api.response.SubscriptionResponse;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SubscriptionServiceTest {

    @Mock
    private ExternalSubscriptionPort externalSubscriptionPort;

    @InjectMocks
    private SubscriptionServiceImpl subscriptionService;

    /* ==================== getSubscription ==================== */

    @Test
    @DisplayName("구독 조회: PRO 플랜 사용 중인 고용주의 구독 정보를 정상 반환한다")
    void getSubscription_ProPlan_Success() {
        // given
        Long userId = 1L;
        when(externalSubscriptionPort.getCurrentPlan(userId)).thenReturn(PlanGrade.PRO);

        // when
        SubscriptionResponse result = subscriptionService.getSubscription(userId);

        // then
        assertThat(result.planGrade()).isEqualTo("PRO");
        assertThat(result.feeRate()).isEqualTo(10.0);
        assertThat(result.monthlyPrice()).isEqualTo(19900);
        verify(externalSubscriptionPort, times(1)).getCurrentPlan(userId);
    }

    @Test
    @DisplayName("구독 조회: BASIC 플랜(무료) 사용 중인 고용주의 구독 정보를 정상 반환한다")
    void getSubscription_BasicPlan_Success() {
        // given
        Long userId = 2L;
        when(externalSubscriptionPort.getCurrentPlan(userId)).thenReturn(PlanGrade.BASIC);

        // when
        SubscriptionResponse result = subscriptionService.getSubscription(userId);

        // then
        assertThat(result.planGrade()).isEqualTo("BASIC");
        assertThat(result.feeRate()).isEqualTo(12.0);
        assertThat(result.monthlyPrice()).isEqualTo(0);
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

    /* ==================== changePlan ==================== */

    @Test
    @DisplayName("구독 변경: BASIC -> PRIME 변경 요청 시 정상 처리된다")
    void changePlan_BasicToPrime_Success() {
        // given
        Long userId = 1L;
        SubscriptionChangeRequest request = new SubscriptionChangeRequest("PRIME");
        when(externalSubscriptionPort.getCurrentPlan(userId)).thenReturn(PlanGrade.BASIC);

        // when
        subscriptionService.changePlan(userId, request);

        // then
        verify(externalSubscriptionPort, times(1)).changePlan(userId, PlanGrade.PRIME);
    }

    @Test
    @DisplayName("구독 변경: 현재와 동일한 플랜으로 변경 시 예외가 발생한다")
    void changePlan_SamePlan_ThrowsException() {
        // given
        Long userId = 1L;
        SubscriptionChangeRequest request = new SubscriptionChangeRequest("PRO");
        when(externalSubscriptionPort.getCurrentPlan(userId)).thenReturn(PlanGrade.PRO);

        // then
        assertThatThrownBy(() -> subscriptionService.changePlan(userId, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("동일한 플랜");
    }

    @Test
    @DisplayName("구독 변경: 유효하지 않은 플랜 이름 입력 시 예외가 발생한다")
    void changePlan_InvalidPlanName_ThrowsException() {
        // given
        Long userId = 1L;
        SubscriptionChangeRequest request = new SubscriptionChangeRequest("GOLD");
        // PlanGrade.valueOf가 먼저 실패하므로 getCurrentPlan 호출 없음 -> stubbing 불필요

        // then
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

    /* ==================== cancelSubscription ==================== */

    @Test
    @DisplayName("구독 취소: PRO 플랜 사용 중인 고용주가 취소 요청 시 정상 처리된다")
    void cancelSubscription_ProPlan_Success() {
        // given
        Long userId = 1L;
        SubscriptionCancelRequest request = new SubscriptionCancelRequest("서비스 이용 빈도 감소");
        when(externalSubscriptionPort.getCurrentPlan(userId)).thenReturn(PlanGrade.PRO);

        // when
        subscriptionService.cancelSubscription(userId, request);

        // then
        verify(externalSubscriptionPort, times(1)).cancelSubscription(userId, "서비스 이용 빈도 감소");
    }

    @Test
    @DisplayName("구독 취소: 이미 BASIC 플랜 사용 중이면 예외가 발생한다")
    void cancelSubscription_AlreadyBasicPlan_ThrowsException() {
        // given
        Long userId = 1L;
        SubscriptionCancelRequest request = new SubscriptionCancelRequest(null);
        when(externalSubscriptionPort.getCurrentPlan(userId)).thenReturn(PlanGrade.BASIC);

        // then
        assertThatThrownBy(() -> subscriptionService.cancelSubscription(userId, request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("무료(BASIC) 플랜");
    }

    @Test
    @DisplayName("구독 취소: request가 null이어도 정상 처리된다 (reason 없이 취소)")
    void cancelSubscription_NullRequest_Success() {
        // given
        Long userId = 1L;
        when(externalSubscriptionPort.getCurrentPlan(userId)).thenReturn(PlanGrade.PRIME);

        // when
        subscriptionService.cancelSubscription(userId, null);

        // then
        verify(externalSubscriptionPort, times(1)).cancelSubscription(userId, null);
    }
}
