package com.fallguys.mypage.entity.employer;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Getter
@Table(name = "employer",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_employer_user", columnNames = "user_id")
        })
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Employer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "employer_id")
    private Long employerId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

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
    *   생성
    *  =========================*/
    public static Employer create(
            Long userId,
            Subscription subscription,
            String companyName,
            String businessRegistrationNumber,
            String description,
            String logoUrl
    ) {
        if (userId == null) throw new IllegalArgumentException("userId 가 비어있습니다.");
        Objects.requireNonNull(subscription, "subscription 이 비어있습니다.");

        Employer employer = new Employer();
        employer.userId = userId;
        employer.subscription = subscription;
        employer.companyName = normalize(companyName);
        employer.businessRegistrationNumber = normalizeBrn(businessRegistrationNumber);
        employer.description = normalizeNullable(description);
        employer.logoUrl = normalizeNullable(logoUrl);

        employer.validateInvariants();
        return employer;
    }


    /* =========================
     *   업데이트
     *  =========================*/
    public void updateProfile(String companyName, String businessRegistrationNumber, String description, String logoUrl) {
        this.companyName = normalize(companyName);
        this.businessRegistrationNumber = normalizeBrn(businessRegistrationNumber);
        this.description = normalizeNullable(description);
        this.logoUrl = normalizeNullable(logoUrl);

        validateInvariants();
    }

    public void changeSubscription(Subscription subscription) {
        this.subscription = Objects.requireNonNull(subscription, "subscription 이 비어있습니다.");
    }

    public void changeLogoUrl(String logoUrl) {
        this.logoUrl = normalizeNullable(logoUrl);
    }

    public void changeDescription(String description) {
        this.description = normalizeNullable(description);
        validateInvariants();
    }

    /* =========================
     *   도메인 규칙/검증
     *  =========================*/
    private void validateInvariants() {
        if (companyName == null || companyName.isBlank()) {
            throw new IllegalArgumentException("companyName 이 비어있습니다.");
        }
        if (businessRegistrationNumber == null || businessRegistrationNumber.isBlank()) {
            throw new IllegalArgumentException("businessRegistrationNumber 이 비어있습니다.");
        }
        if (!businessRegistrationNumber.matches("\\d{10}")) {
            throw new IllegalArgumentException("businessRegistrationNumber 은 반드시 10자(숫자)여야 합니다.");
        }
        if (description != null && description.length() > 5000) {
            throw new IllegalArgumentException("description 은 5000 <= char 이어야 합니다.");
        }
    }

    private static String normalize(String value) {
        if (value == null) throw new IllegalArgumentException("값이 비어있습니다.");
        return value.trim();
    }

    private static String normalizeNullable(String value) {
        if (value == null) return null;
        String v = value.trim();
        return v.isEmpty() ? null : v;
    }

    private static String normalizeBrn(String brn) {
        String v = normalize(brn);
        return v.replace("-", "");
    }
}
