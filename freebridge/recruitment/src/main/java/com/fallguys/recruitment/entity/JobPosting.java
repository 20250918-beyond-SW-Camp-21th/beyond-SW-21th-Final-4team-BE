package com.fallguys.recruitment.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
@Entity
@Table(name = "job_posting")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
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
    private JobPostingStatus status=JobPostingStatus.OPEN;

    @Column(nullable = false)
    private LocalDateTime createdAt=LocalDateTime.now();

    @Column(nullable = false)
    private LocalDateTime updatedAt;

}