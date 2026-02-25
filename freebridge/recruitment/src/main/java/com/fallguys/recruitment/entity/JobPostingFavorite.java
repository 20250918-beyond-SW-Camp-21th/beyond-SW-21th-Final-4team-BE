package com.fallguys.recruitment.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "job_posting_favorite",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_job_posting_favorite_freelancer_posting",
                columnNames = {"freelancer_id", "job_posting_id"}
        )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class JobPostingFavorite {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "freelancer_id", nullable = false)
    private Long freelancerId;

    @Column(name = "job_posting_id", nullable = false)
    private Long jobPostingId;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public static JobPostingFavorite of(Long freelancerId, Long jobPostingId) {
        JobPostingFavorite favorite = new JobPostingFavorite();
        favorite.freelancerId = freelancerId;
        favorite.jobPostingId = jobPostingId;
        return favorite;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
