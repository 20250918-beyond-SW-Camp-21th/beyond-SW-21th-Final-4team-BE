package com.fallguys.recruitment.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;


public class Project {


    private String id = UUID.randomUUID().toString();

    private String jobPostingId;

    private String employerId;

    private String freelancerId;

    private String projectName;

    private LocalDate startDate;
    private LocalDate endDate;


    private ProjectStatus status = ProjectStatus.IN_PROGRESS;


    private LocalDateTime createdAt = LocalDateTime.now();


    private LocalDateTime updatedAt = LocalDateTime.now();
}
