package com.fallguys.mypage.service.employer;

import com.fallguys.common.port.FileStorage;
import com.fallguys.mypage.dto.employer.request.EmployerProfileUpdateRequestDto;
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
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class EmployerProfileService {

    private final EmployerRepository employerRepository;
    private final ProjectRepository projectRepository;

    @Transactional(readOnly = true)
    public EmployerProfileResponseDto getEmployerProfile(String userId) {
        Long parsedUserId = Long.parseLong(userId); // TODO: 적절한 예외 처리 필요

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

        // 2. Project Status
        int completedProjects = projectRepository.countCompletedProjectsByEmployerId(employer.getEmployerId());
        int inProgressProjects = projectRepository.countInProgressProjectsByEmployerId(employer.getEmployerId());

        EmployerProjectStatusDto projectStatus = new EmployerProjectStatusDto(
                0, // recruitingProjects (추후 연동 필요)
                0, // reviewingProjects (추후 연동 필요)
                inProgressProjects,
                completedProjects
        );

        // 3. Ratings (추후 Review 도메인과 연동 필요)
        EmployerRatingDto ratings = new EmployerRatingDto(
                0.0, // averageRate
                0.0, // atmosphereRate
                0.0, // salarySatisfactionRate
                0.0  // scheduleAdherenceRate
        );

        return new EmployerProfileResponseDto(basicProfile, ratings, projectStatus);
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
