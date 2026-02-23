package com.fallguys.recruitment.api.dto.request;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class JobPostingCreateDTO {
    Long employerId;
    String employerName;
    String title;
    String description;
    List<String> techStack;
    Long budget;
    Integer duration;
}
