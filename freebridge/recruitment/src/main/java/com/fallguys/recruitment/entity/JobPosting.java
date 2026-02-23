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

    @Column(nullable=false)
    private Status status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private JobPostingStatus postingStatus = JobPostingStatus.OPEN;

    public static JobPosting from(JobPostingCreateDTO jobPostingCreateDTO, Long employerId, String employerName) {
        JobPosting jobPosting = new JobPosting();
        jobPosting.create(jobPostingCreateDTO, employerId, employerName);
        return jobPosting;
    }

    public void update(JobPostingUpdateDTO jobPostingUpdateDTO) {
        this.title = jobPostingUpdateDTO.getTitle();
        this.description = jobPostingUpdateDTO.getDescription();
        this.techStack = jobPostingUpdateDTO.getTechStack() == null
                ? new ArrayList<>()
                : new ArrayList<>(jobPostingUpdateDTO.getTechStack());
        this.budget = jobPostingUpdateDTO.getBudget();
        this.duration = jobPostingUpdateDTO.getDuration();
        this.postingStatus = jobPostingUpdateDTO.getStatus();
    }

    private void create(JobPostingCreateDTO jobPostingCreateDTO, Long employerId, String employerName) {
        this.title = jobPostingCreateDTO.getTitle();
        this.description = jobPostingCreateDTO.getDescription();
        this.techStack = jobPostingCreateDTO.getTechStack() == null
                ? new ArrayList<>()
                : new ArrayList<>(jobPostingCreateDTO.getTechStack());
        this.budget = jobPostingCreateDTO.getBudget();
        this.duration = jobPostingCreateDTO.getDuration();
        this.postingStatus = JobPostingStatus.OPEN;
        this.status=Status.ACTIVE;
        assignEmployer(employerId);
        this.employerName = employerName;
    }

    public void delete() {
        this.status = Status.DELETED;
    }
}
