package com.fallguys.recruitment.entity;

import com.fallguys.recruitment.api.dto.request.JobPostingCreateDTO;
import com.fallguys.recruitment.api.dto.request.JobPostingUpdateDTO;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "job_posting")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class JobPosting extends BaseEntity {

    @Column(name = "employer_name", nullable = false)
    private String employerName;

    @Column(nullable = false)
    private String title;

    @Lob
    @Column(nullable = false)
    private String description;

    @ElementCollection
    @CollectionTable(name = "job_posting_tech_stack", joinColumns = @JoinColumn(name = "job_posting_id"))
    @Column(name = "tech", nullable = false)
    private List<String> techStack = new ArrayList<>();

    @Column(nullable = false)
    private Long budget;

    @Column(nullable = false)
    private Integer duration;

    @Enumerated(EnumType.STRING)
    @Column(nullable=false)
    private Status status=Status.ACTIVE;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private JobPostingStatus postingStatus = JobPostingStatus.OPEN;

    public static JobPosting from(JobPostingCreateDTO jobPostingCreateDTO, Long employerId, String employerName) {
        JobPosting jobPosting = new JobPosting();
        jobPosting.create(jobPostingCreateDTO, employerId, employerName);
        return jobPosting;
    }

    public void update(JobPostingUpdateDTO dto) {
        this.title = dto.title() == null ? this.title : dto.title();
        this.description = dto.description() == null ? this.description : dto.description();
        this.techStack = dto.techStack() == null
                ? this.techStack
                : new ArrayList<>(dto.techStack());
        this.budget = dto.budget() == null ? this.budget : dto.budget();
        this.duration = dto.duration() == null ? this.duration : dto.duration();
        this.postingStatus = dto.status() == null ? this.postingStatus : dto.status();
    }

    private void create(JobPostingCreateDTO dto,
                        Long employerId,
                        String employerName) {

        this.title = dto.title();
        this.description = dto.description();
        this.techStack = dto.techStack() == null
                ? new ArrayList<>()
                : new ArrayList<>(dto.techStack());
        this.budget = dto.budget();
        this.duration = dto.duration();
        this.postingStatus = JobPostingStatus.OPEN;
        this.status = Status.ACTIVE;

        assignEmployer(employerId);
        this.employerName = employerName;
    }

    public void delete() {
        this.status = Status.DELETED;
    }

    public void markInProgress() {
        this.postingStatus = JobPostingStatus.IN_PROGRESS;
    }
}
