package com.fallguys.mypage.service;

import com.fallguys.mypage.api.shared.SharedMypageApi;
import com.fallguys.mypage.api.web.dto.employer.response.EmployerProfilePreviewResponseDto;
import com.fallguys.mypage.api.web.dto.freelancer.response.FreelancerProfilePreviewResponseDto;
import com.fallguys.mypage.entity.employer.Employer;
import com.fallguys.mypage.entity.freelancer.Freelancer;
import com.fallguys.mypage.entity.freelancer.PortfolioInfo;
import com.fallguys.mypage.entity.resume.Resume;
import com.fallguys.mypage.repository.employer.EmployerRepository;
import com.fallguys.mypage.repository.freelancer.FreelancerRepository;
import com.fallguys.mypage.repository.resume.ResumeRepository;
import com.fallguys.user.api.shared.ExternalUserApi;
import com.fallguys.user.api.shared.response.ExternalUserMyInfoResponse;
import com.fallguys.user.api.shared.response.ExternalUserResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProfilePreviewService {

    private final EmployerRepository employerRepository;
    private final FreelancerRepository freelancerRepository;
    private final ResumeRepository resumeRepository;
    private final ExternalUserApi externalUserApi;
    private final SharedMypageApi sharedMypageApi;

    @Transactional(readOnly = true)
    public EmployerProfilePreviewResponseDto getEmployerPreview(Long employerId) {
        Employer employer = employerRepository.findById(employerId)
                .orElseThrow(() -> new IllegalArgumentException("Employer profile not found"));

        ExternalUserMyInfoResponse userInfo = null;
        try {
            userInfo = externalUserApi.getMyInfo(employer.getUserId());
        } catch (Exception ignored) {
        }

        return new EmployerProfilePreviewResponseDto(
                employer.getEmployerId(),
                employer.getUserId(),
                employer.getCompanyName(),
                employer.getIndustry(),
                employer.getScale() != null ? employer.getScale().name() : null,
                employer.getLocation(),
                employer.getWebsiteUrl(),
                userInfo != null ? userInfo.getPhone() : null,
                employer.getDescription(),
                employer.getLogoUrl()
        );
    }

    @Transactional(readOnly = true)
    public FreelancerProfilePreviewResponseDto getFreelancerPreview(Long freelancerId) {
        Freelancer freelancer = freelancerRepository.findById(freelancerId)
                .orElseThrow(() -> new IllegalArgumentException("Freelancer profile not found"));

        ExternalUserResponse user = sharedMypageApi.getUserById(freelancer.getUserId());
        Resume resume = resumeRepository.findByFreelancerId(freelancerId).orElse(null);
        PortfolioInfo portfolio = freelancer.getPortfolioInfo();

        return new FreelancerProfilePreviewResponseDto(
                freelancer.getFreelancerId(),
                freelancer.getUserId(),
                user != null ? user.getName() : null,
                freelancer.getAvatarUrl(),
                freelancer.getJob(),
                freelancer.getIntroduction(),
                freelancer.getGrade() != null ? freelancer.getGrade().name() : null,
                freelancer.getCareerYears(),
                freelancer.getWage(),
                freelancer.getSkills() == null ? List.of() : freelancer.getSkills(),
                resume != null ? resume.getBirthDate() : null,
                resume != null ? resume.getPhone() : null,
                resume != null ? resume.getEmail() : null,
                resume != null ? resume.getAddress() : null,
                portfolio != null ? portfolio.getPortfolioFileUrl() : null,
                portfolio != null ? portfolio.getPortfolioFileName() : null,
                portfolio != null ? portfolio.getPortfolioLastUpdated() : null
        );
    }
}
