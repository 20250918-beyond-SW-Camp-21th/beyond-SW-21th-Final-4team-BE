package com.fallguys.recruitment.api.dto.request;

import com.fallguys.recruitment.entity.JobPostingStatus;
import java.util.List;

public record JobPostingUpdateDTO() {
    static String title;
    static String description;
    static List<String> techStack;
    static Long budget;
    static Integer duration;
    static JobPostingStatus status;
}
