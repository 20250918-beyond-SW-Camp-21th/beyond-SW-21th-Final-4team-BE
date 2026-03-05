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

    // ?덉빟??援щ룆 蹂寃쎄굔(?ㅼ슫洹몃젅?대뱶) 以? ?곸슜?쇱씠 吏?ш굅???꾨옒??紐⑸줉 議고쉶
    List<Employer> findByPendingSubscriptionIsNotNullAndPlanChangeEffectiveDateLessThanEqual(LocalDateTime effectiveDate);
    List<Employer> findByNextBillingDateLessThanEqualAndBillingKeyIsNotNull(LocalDateTime billingDate);
}
