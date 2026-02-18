package com.fallguys.mypage.entity.employer;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Getter
@Table(name = "employer")
@EntityListeners(AuditingEntityListener.class)
@NoArgsConstructor(access =  AccessLevel.PROTECTED)
public class Employer {

    @Id
    @Column(name = "employer_id")
    private Long employerId; // = user_id (PK 공유)

    @MapsId
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user; // ※ 너희 프로젝트의 User 엔티티 패키지에 맞춰 import/수정

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Subscription subscription;

    @Column(nullable = false, length = 100)
    private String companyName;

    @Column(nullable = false, length = 20, unique = true)
    private String businessRegistrationNumber;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 500)
    private String logoUrl;

    @CreatedDate
    @Column(updatable = false, nullable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    /* =========================
       생성 로직 (정적 팩토리)
       */
    public static Employer create(
            User user,
            Subscription subscription,
            String companyName,
            String businessRegistrationNumber,
            String description,
            String logoUrl
    ) {
        Objects.requireNonNull(user, "user must not be null");
        Objects.requireNonNull(subscription, "subscription must not be null");

        Employer employer = new Employer();
        employer.user = user;
        employer.subscription = subscription;
        employer.companyName = normalize(companyName);
        employer.businessRegistrationNumber = normalizeBrn(businessRegistrationNumber);
        employer.description = normalizeNullable(description);
        employer.logoUrl = normalizeNullable(logoUrl);

        employer.validateInvariants();
        return employer;
    }


    /* =========================
       POJO 스타일 변경 메서드
       */
    public void updateProfile(String companyName, String businessRegistrationNumber, String description, String logoUrl) {
        this.companyName = normalize(companyName);
        this.businessRegistrationNumber = normalizeBrn(businessRegistrationNumber);
        this.description = normalizeNullable(description);
        this.logoUrl = normalizeNullable(logoUrl);

        validateInvariants();
    }

    public void changeSubscription(Subscription subscription) {
        this.subscription = Objects.requireNonNull(subscription, "subscription must not be null");
    }

    public void changeLogoUrl(String logoUrl) {
        this.logoUrl = normalizeNullable(logoUrl);
    }

    public void changeDescription(String description) {
        this.description = normalizeNullable(description);
        validateInvariants();
    }

    /* =========================
       도메인 규칙/검증
       */
    private void validateInvariants() {
        if (companyName.isBlank()) {
            throw new IllegalArgumentException("companyName must not be blank");
        }
        if (businessRegistrationNumber.isBlank()) {
            throw new IllegalArgumentException("businessRegistrationNumber must not be blank");
        }
        // 예: 사업자등록번호(한국) 10자리 숫자만 허용 (하이픈 제거 후)
        if (!businessRegistrationNumber.matches("\\d{10}")) {
            throw new IllegalArgumentException("businessRegistrationNumber must be 10 digits (numbers only)");
        }
        // description 너무 길면 제한하고 싶다면 여기서 체크 (DB TEXT라 무한이지만 서비스 정책상 제한 추천)
        if (description != null && description.length() > 5000) {
            throw new IllegalArgumentException("description must be <= 5000 chars");
        }
    }

    private static String normalize(String value) {
        if (value == null) throw new IllegalArgumentException("value must not be null");
        return value.trim();
    }

    private static String normalizeNullable(String value) {
        if (value == null) return null;
        String v = value.trim();
        return v.isEmpty() ? null : v;
    }

    private static String normalizeBrn(String brn) {
        String v = normalize(brn);
        // "123-45-67890" 입력도 허용하려면 하이픈 제거
        return v.replace("-", "");
    }
}
