package com.fallguys.recruitment.api.dto.request;

import java.time.LocalDateTime;
import java.util.List;

public class JobPostingCreateDTO {
    Long employerId;
    String employerName;
    String title;
    String description;
    List<String> techStack;
    Long budger;
    Integer duration;
    LocalDateTime createdAt=LocalDateTime.now();
}
