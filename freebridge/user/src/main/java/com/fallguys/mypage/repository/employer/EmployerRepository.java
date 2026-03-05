package com.fallguys.mypage.repository.employer;

import com.fallguys.mypage.entity.employer.Employer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface EmployerRepository extends JpaRepository<Employer, Long> {

    Optional<Employer> findByUserId(Long userId);

    // 예약된 구독 변경건(다운그레이드) 중, 적용일이 지났거나 도래한 목록 조회
    List<Employer> findByPendingSubscriptionIsNotNullAndPlanChangeEffectiveDateLessThanEqual(LocalDateTime effectiveDate);
}
