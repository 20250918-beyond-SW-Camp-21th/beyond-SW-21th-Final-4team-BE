package com.fallguys.recruitment.api.dto;

import java.time.LocalDateTime;
import java.util.List;

public class JopPostingCreateDTO {
    Long employerId;
    String employerName;
    String title;
    String description;
    List<String> techStack;
    Long budger;
    Integer duration;
    LocalDateTime createdAt=LocalDateTime.now();
}
