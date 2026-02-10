package com.fallguys.recruitment.entity;

import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class Project {

    @Builder.Default
    private String id = UUID.randomUUID().toString();

    private String jobPostingId;

    private String employerId;

    private String freelancerId;

    private String projectName;

    private LocalDate startDate;
    private LocalDate endDate;

    @Builder.Default
    private ProjectStatus status = ProjectStatus.IN_PROGRESS;

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();
}
