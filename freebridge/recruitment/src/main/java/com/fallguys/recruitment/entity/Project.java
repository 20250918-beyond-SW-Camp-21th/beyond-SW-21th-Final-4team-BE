package com.fallguys.recruitment.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(
        name = "projects",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_project_job_posting_freelancer",
                columnNames = {"job_posting_id", "freelancer_id"}
        )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Project extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_posting_id", nullable = false)
    private JobPosting jobPosting;

    @Column(name = "freelancer_id", nullable = false)
    private Long freelancerId;

    @Column(nullable = false)
    private String projectName;

    @Column(name = "headcount", nullable = false)
    private Integer headcount;

    @Column
    private LocalDate startDate;

    @Column
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ProjectStatus status = ProjectStatus.IN_PROGRESS;

    public static Project create(JobPosting jobPosting, Long freelancerId) {
        Project project = new Project();
        project.jobPosting = jobPosting;
        project.freelancerId = freelancerId;
        project.projectName = jobPosting.getTitle();
        project.headcount = jobPosting.getHeadcount();
        project.status = ProjectStatus.IN_PROGRESS;
        project.assignEmployer(jobPosting.getEmployerId());
        return project;
    }

    public void complete(){
        if (this.status == ProjectStatus.COMPLETED){
            throw new IllegalStateException("이미 완료된 프로젝트입니다.");
        }
        this.status = ProjectStatus.COMPLETED;
    }

    public void cancel() {
        if (this.status == ProjectStatus.COMPLETED) {
            return;
        }
        this.status = ProjectStatus.CANCELLED;
    }

    public void reopen() {
        if (this.status == ProjectStatus.COMPLETED) {
            throw new IllegalStateException("완료된 프로젝트는 다시 진행할 수 없습니다.");
        }
        this.status = ProjectStatus.IN_PROGRESS;
    }
}
