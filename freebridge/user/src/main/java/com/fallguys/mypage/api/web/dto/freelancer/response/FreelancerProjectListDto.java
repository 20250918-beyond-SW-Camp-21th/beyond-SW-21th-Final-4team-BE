package com.fallguys.mypage.api.web.dto.freelancer.response;

import java.util.List;

public record FreelancerProjectListDto(
        Long projectId,
        String title,
        String employerName,
        String projectStatus,
        String description,
        Long budget,
        List<String> techStack,
        String startDate,
        String endDate
) {}
