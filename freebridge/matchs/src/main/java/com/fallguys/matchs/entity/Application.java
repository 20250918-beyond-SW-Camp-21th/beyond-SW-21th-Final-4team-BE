package com.fallguys.matchs.entity;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

//프리랜서->기업
@Entity
@Table(
        name = "application",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_application_job_posting_freelancer",
                columnNames = {"job_posting_id", "freelancer_id"}
        )
)
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Application {

    @Id
    @GeneratedValue(strategy= GenerationType.IDENTITY)
    private Long id ;

    @Column(nullable=false)
    private Long jobPostingId;

    @Column(nullable=false)
    private Long freelancerId;

    @Column(nullable=false)
    private Long employerId;

    @Column(nullable=false)
    private String message;

    @Column(nullable=false)
    @Builder.Default
    @Enumerated(EnumType.STRING)
    private MatchsStatus status=MatchsStatus.PENDING;

    @Column
    @Builder.Default
    private LocalDateTime createdAt=LocalDateTime.now();

    public static Application create(Long jobPostingId, Long freelancerId, Long employerId, String message) {
        return Application.builder()
                .jobPostingId(jobPostingId)
                .freelancerId(freelancerId)
                .employerId(employerId)
                .message(message)
                .status(MatchsStatus.PENDING)
                .build();
    }

    public void accept() {
        this.status = MatchsStatus.ACCEPTED;
    }

    public void reject() {
        this.status = MatchsStatus.REJECTED;
    }
}
