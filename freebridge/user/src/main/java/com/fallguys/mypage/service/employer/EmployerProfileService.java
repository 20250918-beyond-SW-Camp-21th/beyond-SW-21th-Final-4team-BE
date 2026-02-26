package com.fallguys.mypage.service.employer;

import com.fallguys.mypage.dto.employer.response.CrmAlertsResponseDto;
import com.fallguys.mypage.dto.employer.response.EmployerProfileResponseDto;
import com.fallguys.mypage.dto.employer.response.EmployerBasicProfileDto;
import com.fallguys.mypage.dto.employer.response.EmployerRatingDto;
import com.fallguys.mypage.dto.employer.response.EmployerProjectStatusDto;
import com.fallguys.mypage.entity.employer.Employer;
import com.fallguys.mypage.entity.employer.Subscription;
import com.fallguys.mypage.repository.employer.EmployerRepository;
import com.fallguys.mypage.repository.employer.ProjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EmployerProfileService {

    private final EmployerRepository employerRepository;
    private final ProjectRepository projectRepository;

    public EmployerProfileResponseDto getEmployerProfile(String userIdStr) {
        Long userId = Long.parseLong(userIdStr);
        Employer employer = employerRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("고용주를 찾을 수 없습니다."));

        // Dummy DTO values for other sections
        EmployerBasicProfileDto basicProfile = new EmployerBasicProfileDto(
                employer.getCompanyName(), 
                employer.getIndustry(), 
                employer.getScale().name(), 
                employer.getLocation(), 
                employer.getWebsiteUrl(), 
                employer.getDescription(),
                employer.getLogoUrl(),
                employer.getStatus().name()
        );
        EmployerRatingDto ratings = new EmployerRatingDto(0.0, 0.0, 0.0, 0.0);
        EmployerProjectStatusDto projectStatus = new EmployerProjectStatusDto(0, 0, 0, 0);

        // CRM Flag 계산: FREE 플랜이고 완료된 프로젝트가 1건 이상일 때 프리미엄 업셀 알림 true
        int completedProjects = projectRepository.countCompletedProjectsByEmployerId(employer.getEmployerId());
        boolean isPremiumUpsellEligible = (employer.getSubscription() == Subscription.BASIC || employer.getSubscription() == Subscription.BASIC)
                && completedProjects >= 1;

        CrmAlertsResponseDto crmAlerts = new CrmAlertsResponseDto(isPremiumUpsellEligible);

        return new EmployerProfileResponseDto(basicProfile, ratings, projectStatus, crmAlerts);
    }
}
