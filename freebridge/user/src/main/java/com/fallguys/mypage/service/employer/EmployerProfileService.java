package com.fallguys.mypage.service.employer;

import com.fallguys.mypage.api.web.dto.employer.response.CrmAlertsResponseDto;
import com.fallguys.mypage.api.web.dto.employer.response.EmployerProfileResponseDto;
import com.fallguys.mypage.api.web.dto.employer.response.EmployerBasicProfileDto;
import com.fallguys.mypage.api.web.dto.employer.response.EmployerRatingDto;
import com.fallguys.mypage.api.web.dto.employer.response.EmployerProjectStatusDto;
import com.fallguys.mypage.entity.employer.Employer;
import com.fallguys.mypage.entity.employer.Subscription;
import com.fallguys.mypage.repository.employer.EmployerRepository;
import com.fallguys.mypage.repository.employer.ProjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EmployerProfileService {

    private final EmployerRepository employerRepository;

    @Transactional(readOnly = true)
    public EmployerBasicProfileDto getEmployerProfile(String userId) {
        Long parsedUserId = Long.parseLong(userId);

        Employer employer = employerRepository.findByUserId(parsedUserId)
                .orElseThrow(() -> new RuntimeException("고용주 정보를 찾을 수 없습니다. userId=" + userId));

        // 1. Basic Profile
        EmployerBasicProfileDto basicProfile = new EmployerBasicProfileDto(
                employer.getCompanyName(),
                employer.getIndustry(),
                employer.getScale() != null ? employer.getScale().name() : null,
                employer.getLocation(),
                employer.getWebsiteUrl(),
                employer.getDescription(),
                employer.getLogoUrl(),
                employer.getStatus() != null ? employer.getStatus().name() : null
        );

        return basicProfile;
    }

    @Transactional(readOnly = true)
    public CrmAlertsResponseDto getCrmAlerts(String userId) {
        Long parsedUserId = Long.parseLong(userId);

        Employer employer = employerRepository.findByUserId(parsedUserId)
                .orElseThrow(() -> new RuntimeException("고용주 정보를 찾을 수 없습니다. userId=" + userId));

        int completedProjects = projectRepository.countCompletedProjectsByEmployerId(employer.getEmployerId());

        boolean isUpsellEligible = (employer.getSubscription() == Subscription.BASIC) && (completedProjects > 0);
        return new CrmAlertsResponseDto(isUpsellEligible);
    }
}
