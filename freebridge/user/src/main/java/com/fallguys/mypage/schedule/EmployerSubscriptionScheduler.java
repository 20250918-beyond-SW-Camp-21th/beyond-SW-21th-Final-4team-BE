package com.fallguys.mypage.schedule;

import com.fallguys.mypage.entity.employer.Employer;
import com.fallguys.mypage.repository.employer.EmployerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class EmployerSubscriptionScheduler {

    private final EmployerRepository employerRepository;

    /**
     * 예약된 구독 변경(다운그레이드) 처리 일자가 도래한 고용주들의 구독을 실제 적용하는 스케줄러
     */
    @Scheduled(cron = "0 0 0 * * *") // 자정
    @Transactional
    public void applyScheduledSubscriptions() {
        LocalDateTime now = LocalDateTime.now();
        log.info("[Scheduler] 예약된 구독 다운그레이드 승격 배치를 시작합니다. 기준 시간: {}", now);

        List<Employer> employersToUpdate = employerRepository
                .findByPendingSubscriptionIsNotNullAndPlanChangeEffectiveDateLessThanEqual(now);

        if (employersToUpdate.isEmpty()) {
            log.info("[Scheduler] 적용할 구독 예약 건이 없습니다.");
            return;
        }

        int successCount = 0;
        for (Employer employer : employersToUpdate) {
            try {
                // 다운그레이드/예약 내역을 활성화하고 예약 필드를 null 로 클리어
                employer.applyPendingSubscription();
                successCount++;
                log.info("[Scheduler] 고용주(userId: {})의 구독 플랜이 정상 변경(적용)되었습니다.", employer.getUserId());
            } catch (Exception e) {
                log.error("[Scheduler] 고용주(userId: {})의 구독 적용 중 오류 발생: {}", employer.getUserId(), e.getMessage());
            }
        }

        log.info("[Scheduler] 예약 구독 처리 완료 - 대상: {}건, 성공: {}건", employersToUpdate.size(), successCount);
    }
}
