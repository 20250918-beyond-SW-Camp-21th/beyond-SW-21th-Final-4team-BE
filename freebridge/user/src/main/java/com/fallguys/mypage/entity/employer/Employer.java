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
     * 생성 메소드
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
     * UPDATE 메소드
     * =========================
     */

    public void changeStatus(EmployerStatus newStatus) {
        if (newStatus == null)
            throw new IllegalArgumentException("newStatus is required");
        if (this.status == EmployerStatus.LEFT) {
            throw new IllegalStateException("이미 이탈한 고용주의 상태는 변경할 수 없습니다.");
        }
        this.status = newStatus;
    }

    public void changeCompanyName(String companyName) {
        this.companyName = normalize(companyName, "companyName");
    }

    public void changeSubscription(Subscription subscription) {
        this.subscription = requireNonNull(subscription, "subscription");
        // 즉시 변경 시 예약 내역 초기화
        this.pendingSubscription = null;
        this.planChangeEffectiveDate = null;
    }

    public void scheduleSubscriptionChange(Subscription targetSubscription, LocalDateTime effectiveDate) {
        this.pendingSubscription = requireNonNull(targetSubscription, "targetSubscription");
        this.planChangeEffectiveDate = requireNonNull(effectiveDate, "effectiveDate");
    }

    public void applyPendingSubscription() {
        if (this.pendingSubscription != null) {
            this.subscription = this.pendingSubscription;
            this.pendingSubscription = null;
            this.planChangeEffectiveDate = null;
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
        // URL 정교 검증까지는 과할 수 있어서 기본 정리만
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
     * 내부 유틸
     * =========================
     */
    private static String normalize(String value, String fieldName) {
        if (value == null)
            throw new IllegalArgumentException(fieldName + " 값이 비어있습니다.");
        String v = value.trim();
        if (v.isEmpty())
            throw new IllegalArgumentException(fieldName + " 값이 비어있습니다.");
        return v;
    }

    private static String normalizeNullable(String value) {
        if (value == null)
            return null;
        String v = value.trim();
        return v.isEmpty() ? null : v;
    }

    private static <T> T requireNonNull(T value, String fieldName) {
        if (value == null)
            throw new IllegalArgumentException(fieldName + " 값이 비어있습니다.");
        return value;
    }
}
