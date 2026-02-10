package com.fallguys.recruitment.entity;

import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class JobPosting {

    @Builder.Default
    private String id = UUID.randomUUID().toString(); // UUID String

    private String employerId;

    private String employerName;

    private String title;

    private String description;

    @Builder.Default
    private List<String> techStack = new ArrayList<>();

    private Long budget;

    private Integer duration; //months

    @Builder.Default
    private JobPostingStatus status = JobPostingStatus.OPEN;

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();
}
