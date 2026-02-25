package com.fallguys.mypage.entity.freelancer;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Getter
@Table(name = "freelancer_status_history")
@EntityListeners(AuditingEntityListener.class)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FreelancerStatusHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "freelancer_status_history_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "freelancer_id", nullable = false)
    private Freelancer freelancer;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private FreelancerStatus previousStatus;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private FreelancerStatus currentStatus;

    @Column(length = 255)
    private String reason;

    @CreatedDate
    @Column(updatable = false, nullable = false)
    private LocalDateTime changedAt;

    public static FreelancerStatusHistory create(Freelancer freelancer, FreelancerStatus previousStatus, FreelancerStatus currentStatus, String reason) {
        FreelancerStatusHistory history = new FreelancerStatusHistory();
        history.freelancer = freelancer;
        history.previousStatus = previousStatus;
        history.currentStatus = currentStatus;
        history.reason = reason;
        return history;
    }
}
