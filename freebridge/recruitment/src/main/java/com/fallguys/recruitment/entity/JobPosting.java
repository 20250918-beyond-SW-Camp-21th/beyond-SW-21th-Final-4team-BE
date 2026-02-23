package com.fallguys.recruitment.entity;

import com.fallguys.recruitment.api.dto.request.JobPostingCreateDTO;
import com.fallguys.recruitment.api.dto.request.JobPostingUpdateDTO;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Builder
@Entity
@Table(name = "job_posting")
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class JobPosting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "employer_id", nullable = false, length = 36)
    private Long employerId;

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
    @Builder.Default
    private JobPostingStatus status=JobPostingStatus.OPEN;

    @Column(nullable = false)
    @Builder.Default
    private LocalDateTime createdAt=LocalDateTime.now();

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    public void update(JobPostingUpdateDTO jobPostingUpdateDTO){
        this.title=jobPostingUpdateDTO.getTitle();
        this.description=jobPostingUpdateDTO.getDescription();
        this.techStack=jobPostingUpdateDTO.getTechStack();
        this.budget=jobPostingUpdateDTO.getBudget();
        this.duration=jobPostingUpdateDTO.getDuration();
        this.status=jobPostingUpdateDTO.getStatus();
        this.updatedAt=LocalDateTime.now();
    }

    public void create(JobPostingCreateDTO jobPostingCreateDTO,Long employerId, String employerName){
        this.title=jobPostingCreateDTO.getTitle();
        this.description=jobPostingCreateDTO.getDescription();
        this.techStack=jobPostingCreateDTO.getTechStack();
        this.budget=jobPostingCreateDTO.getBudget();
        this.duration=jobPostingCreateDTO.getDuration();
        this.status=JobPostingStatus.OPEN;
        this.employerId=employerId;
        this.employerName=employerName;
        this.createdAt=LocalDateTime.now();
    }
}