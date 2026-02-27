package com.fallguys.userlike.entity;

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
        name = "employer_freelancer_favorite",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_employer_favorite_employer_freelancer",
                columnNames = {"employer_id", "freelancer_id"}
        )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EmployerFreelancerFavorite {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "employer_id", nullable = false)
    private Long employerId;

    @Column(name = "freelancer_id", nullable = false)
    private Long freelancerId;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public static EmployerFreelancerFavorite of(Long employerId, Long freelancerId) {
        EmployerFreelancerFavorite favorite = new EmployerFreelancerFavorite();
        favorite.employerId = employerId;
        favorite.freelancerId = freelancerId;
        return favorite;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
