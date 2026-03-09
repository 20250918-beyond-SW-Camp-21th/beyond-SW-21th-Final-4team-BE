package com.fallguys.mypage.service.freelancer;

import com.fallguys.common.exception.BusinessException;
import com.fallguys.common.exception.ErrorCode;
import com.fallguys.mypage.api.web.dto.resume.CareerDto;
import com.fallguys.mypage.api.web.dto.resume.CertificationDto;
import com.fallguys.mypage.api.web.dto.resume.EducationDto;
import com.fallguys.mypage.api.web.dto.resume.FreelancerResumeResponseDto;
import com.fallguys.mypage.api.web.dto.resume.request.CareerRequestDto;
import com.fallguys.mypage.api.web.dto.resume.request.CertificationRequestDto;
import com.fallguys.mypage.api.web.dto.resume.request.EducationRequestDto;
import com.fallguys.mypage.api.web.dto.resume.request.ResumeBasicInfoRequestDto;
import com.fallguys.mypage.entity.resume.Career;
import com.fallguys.mypage.entity.resume.Certification;
import com.fallguys.mypage.entity.resume.Education;
import com.fallguys.mypage.entity.resume.EduStatus;
import com.fallguys.mypage.entity.resume.Resume;
import com.fallguys.mypage.repository.resume.ResumeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class FreelancerResumeService {

    private final ResumeRepository resumeRepository;

    @Transactional(readOnly = true)
    public FreelancerResumeResponseDto getResume(Long freelancerId) {
        return resumeRepository.findByFreelancerId(freelancerId)
                .map(this::toResumeDto)
                .orElseGet(() -> new FreelancerResumeResponseDto(
                        Collections.emptyList(), Collections.emptyList(), Collections.emptyList()));
    }

    @Transactional
    public void updateResumeBasicInfo(Long freelancerId, ResumeBasicInfoRequestDto request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
        Resume resume = findByFreelancerIdOrThrow(freelancerId);
        resume.updateBasicInfo(request.name(), request.birthDate(), request.phone(),
                request.email(), request.address());
    }

    @Transactional
    public void addEducation(Long freelancerId, EducationRequestDto request) {
        Resume resume = findByFreelancerIdOrThrow(freelancerId);
        resume.addEducation(toEducationEntity(request));
    }

    @Transactional
    public void updateEducation(Long freelancerId, int index, EducationRequestDto request) {
        Resume resume = findByFreelancerIdOrThrow(freelancerId);
        resume.updateEducation(index, toEducationEntity(request));
    }

    @Transactional
    public void deleteEducation(Long freelancerId, int index) {
        Resume resume = findByFreelancerIdOrThrow(freelancerId);
        resume.removeEducation(index);
    }

    @Transactional
    public void addCareer(Long freelancerId, CareerRequestDto request) {
        Resume resume = findByFreelancerIdOrThrow(freelancerId);
        resume.addCareer(toCareerEntity(request));
    }

    @Transactional
    public void updateCareer(Long freelancerId, int index, CareerRequestDto request) {
        Resume resume = findByFreelancerIdOrThrow(freelancerId);
        resume.updateCareerEntry(index, toCareerEntity(request));
    }

    @Transactional
    public void deleteCareer(Long freelancerId, int index) {
        Resume resume = findByFreelancerIdOrThrow(freelancerId);
        resume.removeCareer(index);
    }

    @Transactional
    public void addCertification(Long freelancerId, CertificationRequestDto request) {
        Resume resume = findByFreelancerIdOrThrow(freelancerId);
        resume.addCertification(toCertificationEntity(request));
    }

    @Transactional
    public void updateCertification(Long freelancerId, int index, CertificationRequestDto request) {
        Resume resume = findByFreelancerIdOrThrow(freelancerId);
        resume.updateCertification(index, toCertificationEntity(request));
    }

    @Transactional
    public void deleteCertification(Long freelancerId, int index) {
        Resume resume = findByFreelancerIdOrThrow(freelancerId);
        resume.removeCertification(index);
    }

    private Education toEducationEntity(EducationRequestDto e) {
        if (e == null) throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        EduStatus status = null;
        if (e.eduStatus() != null && !e.eduStatus().isBlank()) {
            try {
                status = EduStatus.valueOf(e.eduStatus().toUpperCase());
            } catch (IllegalArgumentException ex) {
                throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
            }
        }
        return new Education(e.schoolType(), e.schoolName(), e.major(), status, e.entranceDate(), e.graduationDate());
    }

    private Career toCareerEntity(CareerRequestDto c) {
        if (c == null) throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        return new Career(c.companyName(), c.department(), c.position(),
                c.jobType(), c.employmentType(), c.startDate(), c.endDate(), c.description());
    }

    private Certification toCertificationEntity(CertificationRequestDto cert) {
        if (cert == null) throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        return new Certification(cert.name(), cert.issuer(), cert.acquisitionDate());
    }

    private Resume findByFreelancerIdOrThrow(Long freelancerId) {
        return resumeRepository.findByFreelancerId(freelancerId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }

    private FreelancerResumeResponseDto toResumeDto(Resume resume) {
        List<EducationDto> educations = resume.getEducations() == null ? Collections.emptyList() :
                resume.getEducations().stream()
                        .filter(Objects::nonNull)
                        .map(e -> new EducationDto(null, e.getSchoolName(), e.getMajor(),
                                e.getEntranceDate() != null ? e.getEntranceDate().toString() : null,
                                e.getGraduationDate() != null ? e.getGraduationDate().toString() : null,
                                e.getEduStatus() != null ? e.getEduStatus().name() : null))
                        .toList();

        List<CareerDto> careers = resume.getCareers() == null ? Collections.emptyList() :
                resume.getCareers().stream()
                        .filter(Objects::nonNull)
                        .map(c -> new CareerDto(null, c.getCompanyName(), c.getPosition(),
                                c.getStartDate() != null ? c.getStartDate().toString() : null,
                                c.getEndDate() != null ? c.getEndDate().toString() : null,
                                c.getDescription()))
                        .toList();

        List<CertificationDto> certifications = resume.getCertifications() == null ? Collections.emptyList() :
                resume.getCertifications().stream()
                        .filter(Objects::nonNull)
                        .map(cert -> new CertificationDto(null, cert.getName(), cert.getIssuer(),
                                cert.getAcquisitionDate() != null ? cert.getAcquisitionDate().toString() : null))
                        .toList();

        return new FreelancerResumeResponseDto(educations, careers, certifications);
    }
}
