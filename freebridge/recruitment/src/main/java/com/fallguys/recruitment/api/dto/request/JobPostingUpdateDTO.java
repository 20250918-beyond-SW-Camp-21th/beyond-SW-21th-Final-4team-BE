package com.fallguys.recruitment.api.dto.request;

import com.fallguys.recruitment.entity.JobPostingStatus;
import lombok.Getter;
import java.util.List;

@Getter
public class JobPostingUpdateDTO {
    String title;
    String description;
    List<String> techStack;
    Long budget;
    Integer duration;
    JobPostingStatus status;
}
