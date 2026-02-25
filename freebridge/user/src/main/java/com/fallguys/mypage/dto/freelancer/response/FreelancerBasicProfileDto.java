package com.fallguys.mypage.dto.freelancer.response;

import java.util.List;

public record FreelancerBasicProfileDto(
        String avatarUrl,
        String name,
        String job,
        String introduction,
        String grade, // FreelancerGrade enum name
        Integer careerYears,
        Long wage, // 시급
        List<String> skills,
        String status // POTENTIAL, CONTRACTING 등
) {}
