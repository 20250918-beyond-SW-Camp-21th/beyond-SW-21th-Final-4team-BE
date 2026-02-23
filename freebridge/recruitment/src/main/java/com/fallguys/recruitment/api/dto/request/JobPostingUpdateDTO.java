package com.fallguys.recruitment.api.dto.request;

import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
public class JobPostingUpdateDTO {
    String title;
    String description;
    List<String> techStack;
    Long budget;
    Integer duration;
    LocalDateTime updatedAt=LocalDateTime.now();
}
