package com.fallguys.mypage.service;

import com.fallguys.common.port.FileStorage;
import com.fallguys.mypage.api.shared.SharedMypageApi;
import com.fallguys.mypage.api.web.dto.employer.response.EmployerProfilePreviewResponseDto;
import com.fallguys.mypage.api.web.dto.freelancer.response.FreelancerPreviewCareerDto;
import com.fallguys.mypage.api.web.dto.freelancer.response.FreelancerPreviewCertificationDto;
import com.fallguys.mypage.api.web.dto.freelancer.response.FreelancerPreviewEducationDto;
import com.fallguys.mypage.api.web.dto.freelancer.response.FreelancerProfilePreviewResponseDto;
import com.fallguys.mypage.entity.employer.Employer;
import com.fallguys.mypage.entity.freelancer.Freelancer;
import com.fallguys.mypage.entity.freelancer.PortfolioInfo;
import com.fallguys.mypage.entity.resume.Career;
import com.fallguys.mypage.entity.resume.Certification;
import com.fallguys.mypage.entity.resume.Education;
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
    private final FileStorage fileStorage;

    @Transactional(readOnly = true)
    public EmployerProfilePreviewResponseDto getEmployerPreview(Long employerId) {
        Employer employer = employerRepository.findById(employerId)
                .or(() -> employerRepository.findByUserId(employerId))
                .orElse(null);

        if (employer == null) {
            ExternalUserResponse user = null;
            try {
                user = sharedMypageApi.getUserById(employerId);
            } catch (Exception ignored) {
            }

            return new EmployerProfilePreviewResponseDto(
                    null,
                    employerId,
                    user != null ? user.getName() : "기업 정보 없음",
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null
            );
        }

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
                toAccessibleUrl(employer.getLogoUrl())
        );
    }

    @Transactional(readOnly = true)
    public FreelancerProfilePreviewResponseDto getFreelancerPreview(Long freelancerId) {
        Freelancer freelancer = freelancerRepository.findById(freelancerId)
                .or(() -> freelancerRepository.findByUserId(freelancerId))
                .orElseThrow(() -> new IllegalArgumentException("Freelancer profile not found"));

        ExternalUserResponse user = null;
        try {
            user = sharedMypageApi.getUserById(freelancer.getUserId());
        } catch (Exception ignored) {
        }
        Resume resume = resumeRepository.findByFreelancerId(freelancerId).orElse(null);
        if (resume == null) {
            resume = resumeRepository.findByUserId(freelancer.getUserId()).orElse(null);
        }
        PortfolioInfo portfolio = freelancer.getPortfolioInfo();

        return new FreelancerProfilePreviewResponseDto(
                freelancer.getFreelancerId(),
                freelancer.getUserId(),
                user != null ? user.getName() : null,
                toAccessibleUrl(freelancer.getAvatarUrl()),
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
                resume != null ? mapEducations(resume.getEducations()) : List.of(),
                resume != null ? mapCareers(resume.getCareers()) : List.of(),
                resume != null ? mapCertifications(resume.getCertifications()) : List.of(),
                portfolio != null ? toAccessibleUrl(portfolio.getPortfolioFileUrl()) : null,
                portfolio != null ? portfolio.getPortfolioFileName() : null,
                portfolio != null ? portfolio.getPortfolioLastUpdated() : null
        );
    }

    private List<FreelancerPreviewEducationDto> mapEducations(List<Education> educations) {
        if (educations == null || educations.isEmpty()) {
            return List.of();
        }

        return educations.stream()
                .map(education -> new FreelancerPreviewEducationDto(
                        education.getSchoolType(),
                        education.getSchoolName(),
                        education.getMajor(),
                        education.getEduStatus() != null ? education.getEduStatus().name() : null,
                        education.getEntranceDate(),
                        education.getGraduationDate()
                ))
                .toList();
    }

    private List<FreelancerPreviewCareerDto> mapCareers(List<Career> careers) {
        if (careers == null || careers.isEmpty()) {
            return List.of();
        }

        return careers.stream()
                .map(career -> new FreelancerPreviewCareerDto(
                        career.getCompanyName(),
                        career.getDepartment(),
                        career.getPosition(),
                        career.getJobType(),
                        career.getEmploymentType(),
                        career.getStartDate(),
                        career.getEndDate(),
                        career.getDescription()
                ))
                .toList();
    }

    private List<FreelancerPreviewCertificationDto> mapCertifications(List<Certification> certifications) {
        if (certifications == null || certifications.isEmpty()) {
            return List.of();
        }

        return certifications.stream()
                .map(certification -> new FreelancerPreviewCertificationDto(
                        certification.getName(),
                        certification.getIssuer(),
                        certification.getAcquisitionDate()
                ))
                .toList();
    }

    private String toAccessibleUrl(String storedKeyOrUrl) {
        if (storedKeyOrUrl == null || storedKeyOrUrl.isBlank()) {
            return null;
        }
        if (storedKeyOrUrl.startsWith("http://") || storedKeyOrUrl.startsWith("https://")) {
            return storedKeyOrUrl;
        }
        return fileStorage.generatePresignedUrl(storedKeyOrUrl);
    }
}
