package com.fallguys.recruitment.api.dto.request;

import com.fallguys.recruitment.entity.JobPostingStatus;

import java.util.List;

public record JobPostingCreateDTO() {
    static Long employerId;
    static String employerName;
    static String title;
    static String description;
    static JobPostingStatus status;
    static List<String> techStack;
    static Long budget;
    static Integer duration;
}
