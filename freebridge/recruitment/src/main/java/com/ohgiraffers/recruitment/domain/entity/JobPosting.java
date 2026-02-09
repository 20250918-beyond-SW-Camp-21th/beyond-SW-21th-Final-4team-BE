package com.ohgiraffers.recruitment.domain.entity;

import com.ohgiraffers.recruitment.domain.enums.JobPostingStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "job_postings")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class JobPosting {

    @Id
    @Column(length = 36)
    private String id; // UUID String

    @Column(name = "employer_id", nullable = false, length = 36)
    private String employerId;

    @Column(name = "employer_name", nullable = false)
    private String employerName;

    @Column(nullable = false)
    private String title;

    @Lob
    @Column(nullable = false)
    private String description;

    @ElementCollection
    @CollectionTable(
            name = "job_posting_tech_stack",
            joinColumns = @JoinColumn(name = "job_posting_id")
    )
    @Column(name = "tech", nullable = false)
    private List<String> techStack = new ArrayList<>();

    @Column(nullable = false)
    private Long budget;

    @Column(nullable = false)
    private Integer duration; // weeks

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private JobPostingStatus status;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    /* =========================
       연관관계 (선택)
       ========================= */
    @OneToMany(mappedBy = "jobPosting", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<JobPosting> projects = new ArrayList<>();

    @PrePersist
    void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.status == null) {
            this.status = JobPostingStatus.OPEN;
        }
    }

    @PreUpdate
    void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}