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
@Table(name = "employer_freelancer_reviews")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmployerReview {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "employer_freelancer_reviews_id")
    private Long id;

    @Column(name = "project_id")
    private Long projectId;

    @Column(name = "employer_id", nullable = false)
    private Long employerId;

    @Column(name = "freelancer_id", nullable = false)
    private Long freelancerId;

    @Column(name = "language")
    private Integer language;

    @Column(name = "framework")
    private Integer framework;

    @Column(name = "debugging")
    private Integer debugging;

    @Column(name = "communication")
    private Integer communication;

    @Column(name = "schedule")
    private Integer schedule;

    @Column(name = "dispute")
    private Integer dispute;

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
            Integer language,
            Integer framework,
            Integer debugging,
            Integer communication,
            Integer schedule,
            Integer dispute,
            String description
    ) {
        this.language = language == null ? this.language : language;
        this.framework = framework == null ? this.framework : framework;
        this.debugging = debugging == null ? this.debugging : debugging;
        this.communication = communication == null ? this.communication : communication;
        this.schedule = schedule == null ? this.schedule : schedule;
        this.dispute = dispute == null ? this.dispute : dispute;
        this.description = description == null ? this.description : description;
    }

    public void softDelete() {
        this.status = ReviewStatus.DELETED;
        this.deleted = true;
    }
}
