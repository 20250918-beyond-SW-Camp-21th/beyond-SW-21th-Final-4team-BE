package com.fallguys.review.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "freelancer_employer_reviews")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FreelancerReview {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "freelancer_employer_reviews_id")
    private Long id;

    @Column(name = "project_id")
    private Long projectId;

    @Column(name = "freelancer_id", nullable = false)
    private Long freelancerId;

    @Column(name = "employer_id", nullable = false)
    private Long employerId;

    @Column(name = "atmosphere")
    private Integer atmosphere;

    @Column(name = "requirement_detail")
    private Integer requirementDetail;

    @Column(name = "schedule")
    private Integer schedule;

    @Column(name = "description")
    private String description;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ReviewStatus status = ReviewStatus.ACTIVE;

    @Builder.Default
    @Column(name = "deleted", nullable = false)
    private Boolean deleted = false;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public void update(
            Integer atmosphere,
            Integer requirementDetail,
            Integer schedule,
            String description
    ) {
        this.atmosphere = atmosphere == null ? this.atmosphere : atmosphere;
        this.requirementDetail = requirementDetail == null ? this.requirementDetail : requirementDetail;
        this.schedule = schedule == null ? this.schedule : schedule;
        this.description = description == null ? this.description : description;
    }

    public void softDelete() {
        this.status = ReviewStatus.DELETED;
        this.deleted = true;
    }
}
