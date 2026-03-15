package com.fallguys.mypage.api.web.dto.freelancer.response;

import java.util.List;

public record FreelancerBasicProfileDto(
        String avatarUrl,
        String name,
        String email,
        String phone,
        String job,
        String introduction,
        String grade, // FreelancerGrade enum name
        Integer careerYears,
        Long wage, // ?щ쭩 ?④?
        List<String> skills,
        String status, // POTENTIAL, CONTRACTING ??
        WorkConditionsDto workConditions,
        ExpertiseDto expertise,
        CollaborationDto collaboration,
        Double averageRating,
        PortfolioInfoDto portfolio,
        CrmAlertsDto crmAlerts
) {}