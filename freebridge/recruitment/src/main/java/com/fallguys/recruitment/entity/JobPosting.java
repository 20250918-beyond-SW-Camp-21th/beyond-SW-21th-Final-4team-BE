package com.fallguys.recruitment.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class JobPosting {

    private String id = UUID.randomUUID().toString();
    private final String employerId;
    private final String employerName;

    private String title;
    private String description;
    private List<String> techStack;

    private Long budget;
    private Integer duration;

    private JobPostingStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public JobPosting(
            String employerId,
            String employerName,
            String title,
            String description,
            List<String> techStack,
            Long budget,
            Integer duration
    ) {
        this.id = UUID.randomUUID().toString();
        this.employerId = employerId;
        this.employerName = employerName;
        this.title = title;
        this.description = description;
        this.techStack = new ArrayList<>(techStack);
        this.budget = budget;
        this.duration = duration;
        this.status = JobPostingStatus.OPEN;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
    }

    /* ===== 도메인 행위 ===== */

    public void close() {
        if (status != JobPostingStatus.OPEN) {
            throw new IllegalStateException("이미 종료된 공고입니다.");
        }
        this.status = JobPostingStatus.CLOSED;
        touch();
    }

    public void updatePosting(
            String title,
            String description,
            List<String> techStack,
            Long budget,
            Integer duration) {
        this.title = title;
        this.description = description;
        this.techStack = new ArrayList<>(techStack);
        this.budget = budget;
        this.duration = duration;
        touch();
    }

    private void touch() {
        this.updatedAt = LocalDateTime.now();
    }

    public boolean isOpen() {
        return status == JobPostingStatus.OPEN;
    }
}
