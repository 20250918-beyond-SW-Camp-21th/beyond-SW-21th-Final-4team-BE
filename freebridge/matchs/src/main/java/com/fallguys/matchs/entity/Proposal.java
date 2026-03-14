package com.fallguys.matchs.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

//기업->프리랜서
@Entity
@Table(
        name = "Proposal",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_proposal_job_posting_freelancer",
                columnNames = {"job_posting_id", "freelancer_id"}
        )
)
@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class Proposal {

    @Id
    @GeneratedValue(strategy= GenerationType.IDENTITY)
    private Long id;

    @Column(nullable=false)
    private Long jobPostingId;

    @Column(nullable=false)
    private Long freelancerId;

    @Column(nullable=false)
    private Long employerId;

    @Column(nullable=false)
    private String message;

    @Column(nullable=false)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private MatchsStatus status=MatchsStatus.PENDING;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    public static Proposal create(Long jobPostingId, Long freelancerId, Long employerId, String message) {
        return Proposal.builder()
                .jobPostingId(jobPostingId)
                .freelancerId(freelancerId)
                .employerId(employerId)
                .message(message)
                .status(MatchsStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();
    }

    public void accept() {
        this.status = MatchsStatus.ACCEPTED;
    }

    public void reject() {
        this.status = MatchsStatus.REJECTED;
    }

}
