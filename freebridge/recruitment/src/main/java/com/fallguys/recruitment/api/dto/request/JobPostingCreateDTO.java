package com.fallguys.recruitment.api.dto.request;

import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
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
