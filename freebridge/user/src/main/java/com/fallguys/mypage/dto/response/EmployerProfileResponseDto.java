package com.fallguys.mypage.dto.response;

import com.fallguys.mypage.entity.employer.Scale;
import com.fallguys.mypage.entity.employer.Subscription;

public record EmployerProfileResponseDto(
        Subscription subscription,
        String companyName,
        String description,
        String logoUrl,
        String industry,
        Scale scale,
        String location,
        String websiteUrl
) {
    public EmployerProfileResponseDto(
            Subscription subscription,
            String companyName,
            String description,
            String logoUrl,
            String industry,
            Scale scale,
            String location,
            String websiteUrl) {
        this.subscription = Subscription.BASIC;
        this.companyName = companyName;
        this.description = description;
        this.logoUrl = logoUrl;
        this.industry = industry;
        this.scale = scale;
        this.location = location;
        this.websiteUrl = websiteUrl;
    }
}