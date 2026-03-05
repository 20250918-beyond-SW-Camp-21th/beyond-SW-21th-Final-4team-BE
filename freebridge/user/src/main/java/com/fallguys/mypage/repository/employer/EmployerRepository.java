package com.fallguys.mypage.repository.employer;

import com.fallguys.mypage.entity.employer.Employer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface EmployerRepository extends JpaRepository<Employer, Long> {

    Optional<Employer> findByUserId(Long userId);

    // 구독 변경 예정일이 지났고 대기 중인 구독이 있는 사용자 조회
    List<Employer> findByPendingSubscriptionIsNotNullAndPlanChangeEffectiveDateLessThanEqual(LocalDateTime effectiveDate);
    List<Employer> findByNextBillingDateLessThanEqualAndBillingKeyIsNotNull(LocalDateTime billingDate);
    Page<Employer> findByNextBillingDateLessThanEqualAndBillingKeyIsNotNull(LocalDateTime billingDate, Pageable pageable);
}
