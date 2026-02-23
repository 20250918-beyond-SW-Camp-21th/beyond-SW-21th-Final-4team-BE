package com.fallguys.recruitment.api.dto.request;

import com.fallguys.recruitment.entity.JobPostingStatus;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class JobPostingCreateDTO {
    Long employerId;
    String employerName;
    String title;
    String description;
    JobPostingStatus status;
    List<String> techStack;
    Long budget;
    Integer duration;
}
