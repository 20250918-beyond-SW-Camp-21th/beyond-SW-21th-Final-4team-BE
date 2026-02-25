package com.fallguys.mypage.entity.employer;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Getter
@Table(name = "employer_status_history")
@EntityListeners(AuditingEntityListener.class)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EmployerStatusHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "employer_status_history_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employer_id", nullable = false)
    private Employer employer;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private EmployerStatus previousStatus;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private EmployerStatus currentStatus;

    @Column(length = 255)
    private String reason;

    @CreatedDate
    @Column(updatable = false, nullable = false)
    private LocalDateTime changedAt;

    public static EmployerStatusHistory create(Employer employer, EmployerStatus previousStatus, EmployerStatus currentStatus, String reason) {
        EmployerStatusHistory history = new EmployerStatusHistory();
        history.employer = employer;
        history.previousStatus = previousStatus;
        history.currentStatus = currentStatus;
        history.reason = reason;
        return history;
    }
}
