package com.fallguys.mypage.dto;

import com.fallguys.mypage.entity.employer.Scale;
import com.fallguys.mypage.entity.employer.Subscription;

public record EmployerProfileDto(
        Subscription subscription,
        String companyName,
        String description,
        String logoUrl,
        String industry,
        Scale scale,
        String location,
        String websiteUrl
) {}