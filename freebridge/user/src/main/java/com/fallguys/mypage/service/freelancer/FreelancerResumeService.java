package com.fallguys.mypage.service.freelancer;

import com.fallguys.common.exception.BusinessException;
import com.fallguys.common.exception.ErrorCode;
import com.fallguys.mypage.api.web.dto.resume.CareerDto;
import com.fallguys.mypage.api.web.dto.resume.CertificationDto;
import com.fallguys.mypage.api.web.dto.resume.EducationDto;
import com.fallguys.mypage.api.web.dto.resume.FreelancerResumeResponseDto;
import com.fallguys.mypage.api.web.dto.resume.ResumeBasicInfoDto;
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
import java.util.Locale;
import java.util.Objects;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
public class FreelancerResumeService {

    private final ResumeRepository resumeRepository;

    @Transactional(readOnly = true)
    public FreelancerResumeResponseDto getResume(Long userId) {
        return resumeRepository.findByUserId(userId)
                .map(this::toResumeDto)
                .orElseGet(() -> new FreelancerResumeResponseDto(
                        null,
                        Collections.emptyList(),
                        Collections.emptyList(),
                        Collections.emptyList()));
    }

    @Transactional
    public void updateResumeBasicInfo(Long userId, ResumeBasicInfoRequestDto request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
        Resume resume = findByUserIdOrThrow(userId);
        resume.updateBasicInfo(request.name(), request.birthDate(), request.phone(),
                request.email(), request.address());
    }

    @Transactional
    public void addEducation(Long userId, EducationRequestDto request) {
        Resume resume = findByUserIdOrThrow(userId);
        resume.addEducation(toEducationEntity(request));
    }

    @Transactional
    public void updateEducation(Long userId, int index, EducationRequestDto request) {
        Resume resume = findByUserIdOrThrow(userId);
        resume.updateEducation(index, toEducationEntity(request));
    }

    @Transactional
    public void deleteEducation(Long userId, int index) {
        Resume resume = findByUserIdOrThrow(userId);
        resume.removeEducation(index);
    }

    @Transactional
    public void addCareer(Long userId, CareerRequestDto request) {
        Resume resume = findByUserIdOrThrow(userId);
        resume.addCareer(toCareerEntity(request));
    }

    @Transactional
    public void updateCareer(Long userId, int index, CareerRequestDto request) {
        Resume resume = findByUserIdOrThrow(userId);
        resume.updateCareerEntry(index, toCareerEntity(request));
    }

    @Transactional
    public void deleteCareer(Long userId, int index) {
        Resume resume = findByUserIdOrThrow(userId);
        resume.removeCareer(index);
    }

    @Transactional
    public void addCertification(Long userId, CertificationRequestDto request) {
        Resume resume = findByUserIdOrThrow(userId);
        resume.addCertification(toCertificationEntity(request));
    }

    @Transactional
    public void updateCertification(Long userId, int index, CertificationRequestDto request) {
        Resume resume = findByUserIdOrThrow(userId);
        resume.updateCertification(index, toCertificationEntity(request));
    }

    @Transactional
    public void deleteCertification(Long userId, int index) {
        Resume resume = findByUserIdOrThrow(userId);
        resume.removeCertification(index);
    }

    private Education toEducationEntity(EducationRequestDto e) {
        if (e == null) throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        EduStatus status = null;
        if (e.eduStatus() != null && !e.eduStatus().isBlank()) {
            try {
                status = EduStatus.valueOf(e.eduStatus().toUpperCase(Locale.ROOT));
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

    private Resume findByUserIdOrThrow(Long userId) {
        return resumeRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESUME_NOT_FOUND));
    }

    private FreelancerResumeResponseDto toResumeDto(Resume resume) {
        ResumeBasicInfoDto basicInfo = new ResumeBasicInfoDto(
                resume.getName(),
                resume.getBirthDate(),
                resume.getPhone(),
                resume.getEmail(),
                resume.getAddress()
        );

        List<EducationDto> educations = resume.getEducations() == null ? Collections.emptyList() :
                IntStream.range(0, resume.getEducations().size())
                        .mapToObj(index -> {
                            Education e = resume.getEducations().get(index);
                            if (e == null) return null;
                            return new EducationDto((long) index, e.getSchoolType(), e.getSchoolName(), e.getMajor(),
                                    e.getEntranceDate() != null ? e.getEntranceDate().toString() : null,
                                    e.getGraduationDate() != null ? e.getGraduationDate().toString() : null,
                                    e.getEduStatus() != null ? e.getEduStatus().name() : null);
                        })
                        .filter(Objects::nonNull)
                        .toList();

        List<CareerDto> careers = resume.getCareers() == null ? Collections.emptyList() :
                IntStream.range(0, resume.getCareers().size())
                        .mapToObj(index -> {
                            Career c = resume.getCareers().get(index);
                            if (c == null) return null;
                            return new CareerDto((long) index, c.getCompanyName(), c.getDepartment(), c.getPosition(),
                                    c.getJobType(), c.getEmploymentType(),
                                    c.getStartDate() != null ? c.getStartDate().toString() : null,
                                    c.getEndDate() != null ? c.getEndDate().toString() : null,
                                    c.getDescription());
                        })
                        .filter(Objects::nonNull)
                        .toList();

        List<CertificationDto> certifications = resume.getCertifications() == null ? Collections.emptyList() :
                IntStream.range(0, resume.getCertifications().size())
                        .mapToObj(index -> {
                            Certification cert = resume.getCertifications().get(index);
                            if (cert == null) return null;
                            return new CertificationDto((long) index, cert.getName(), cert.getIssuer(),
                                    cert.getAcquisitionDate() != null ? cert.getAcquisitionDate().toString() : null);
                        })
                        .filter(Objects::nonNull)
                        .toList();

        return new FreelancerResumeResponseDto(basicInfo, educations, careers, certifications);
    }
}
