package com.fallguys.mypage.entity.employer;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Getter
@Table(name = "employer")
@EntityListeners(AuditingEntityListener.class)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Employer {

    private static final int MAX_BILLING_KEY_LENGTH = 200;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "employer_id")
    private Long employerId;

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private EmployerStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Subscription subscription;

    @Enumerated(EnumType.STRING)
    @Column(name = "pending_subscription", length = 30)
    private Subscription pendingSubscription;

    @Column(name = "plan_change_effective_date")
    private LocalDateTime planChangeEffectiveDate;

    @Column(name = "billing_key", length = 200)
    private String billingKey;

    @Column(name = "next_billing_date")
    private LocalDateTime nextBillingDate;

    @Column(nullable = false, length = 100)
    private String companyName;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 500)
    private String logoUrl;

    @Column(length = 100)
    private String industry;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Scale scale;

    @Column(length = 100)
    private String location;

    @Column(length = 100)
    private String websiteUrl;

    @CreatedDate
    @Column(updatable = false, nullable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    /*
     * =========================
     * 생성 관련
     * =========================
     */
    public static Employer create(
            Long userId,
            Subscription subscription,
            String companyName,
            Scale scale) {
        Employer e = new Employer();
        e.userId = requireNonNull(userId, "userId");
        e.subscription = requireNonNull(subscription, "subscription");
        e.companyName = normalize(companyName, "companyName");
        e.scale = requireNonNull(scale, "scale");
        e.status = EmployerStatus.POTENTIAL;
        return e;
    }

    /*
     * =========================
     * UPDATE 관련
     * =========================
     */

    public void changeStatus(EmployerStatus newStatus) {
        if (newStatus == null) {
            throw new IllegalArgumentException("newStatus is required");
        }
        if (this.status == EmployerStatus.LEFT) {
            throw new IllegalStateException("이미 탈퇴한 고용주는 상태를 변경할 수 없습니다.");
        }
        this.status = newStatus;
    }

    public void changeCompanyName(String companyName) {
        this.companyName = normalize(companyName, "companyName");
    }

    public void changeSubscription(Subscription subscription) {
        this.subscription = requireNonNull(subscription, "subscription");
        // 구독 변경 예약 정보 초기화
        this.pendingSubscription = null;
        this.planChangeEffectiveDate = null;
    }

    public void updateBillingKey(String billingKey) {
        if (billingKey != null && billingKey.length() > MAX_BILLING_KEY_LENGTH) {
            throw new IllegalArgumentException("billingKey length must be <= " + MAX_BILLING_KEY_LENGTH);
        }
        this.billingKey = normalizeNullable(billingKey);
    }

    public void updateNextBillingDate(LocalDateTime nextBillingDate) {
        this.nextBillingDate = nextBillingDate;
    }

    public void scheduleSubscriptionChange(Subscription targetSubscription, LocalDateTime effectiveDate) {
        this.pendingSubscription = requireNonNull(targetSubscription, "targetSubscription");
        LocalDateTime nonNullEffectiveDate = requireNonNull(effectiveDate, "effectiveDate");

        if (!nonNullEffectiveDate.isAfter(LocalDateTime.now())) {
            throw new IllegalArgumentException("구독 변경 일자(effectiveDate)는 현재 시점보다 이후여야 합니다.");
        }

        this.planChangeEffectiveDate = nonNullEffectiveDate;
    }

    public void applyPendingSubscription() {
        if (this.pendingSubscription != null) {
            LocalDateTime now = LocalDateTime.now();
            if (this.planChangeEffectiveDate == null || !this.planChangeEffectiveDate.isAfter(now)) {
                this.subscription = this.pendingSubscription;
                this.pendingSubscription = null;
                this.planChangeEffectiveDate = null;
            }
        }
    }

    public void changeScale(Scale scale) {
        this.scale = requireNonNull(scale, "scale");
    }

    public void updateIndustry(String industry) {
        this.industry = normalizeNullable(industry);
    }

    public void updateLocation(String location) {
        this.location = normalizeNullable(location);
    }

    public void updateWebsiteUrl(String websiteUrl) {
        // URL 유효성 검증은 상위 레이어에서 처리
        this.websiteUrl = normalizeNullable(websiteUrl);
    }

    public void updateDescription(String description) {
        this.description = normalizeNullable(description);
    }

    public void updateLogoUrl(String logoUrl) {
        this.logoUrl = normalizeNullable(logoUrl);
    }

    public void updateProfile(
            String companyName,
            String industry,
            Scale scale,
            String location,
            String websiteUrl,
            String description,
            String logoUrl) {
        changeCompanyName(companyName);
        updateIndustry(industry);
        changeScale(scale);
        updateLocation(location);
        updateWebsiteUrl(websiteUrl);
        updateDescription(description);
        updateLogoUrl(logoUrl);
    }

    /*
     * =========================
     * 공통 유틸
     * =========================
     */
    private static String normalize(String value, String fieldName) {
        if (value == null) {
            throw new IllegalArgumentException(fieldName + " 필드는 필수입니다.");
        }
        String v = value.trim();
        if (v.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " 필드는 필수입니다.");
        }
        return v;
    }

    private static String normalizeNullable(String value) {
        if (value == null) {
            return null;
        }
        String v = value.trim();
        return v.isEmpty() ? null : v;
    }

    private static <T> T requireNonNull(T value, String fieldName) {
        if (value == null) {
            throw new IllegalArgumentException(fieldName + " 필드는 필수입니다.");
        }
        return value;
    }
}
