package com.fallguys.mypage.service.freelancer;

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

@Service
@RequiredArgsConstructor
public class FreelancerResumeService {

    private final ResumeRepository resumeRepository;

    // ─── 이력서 조회 ──────────────────────────────────────────────

    @Transactional(readOnly = true)
    public FreelancerResumeResponseDto getResume(Long freelancerId) {
        return resumeRepository.findByFreelancerId(freelancerId)
                .map(this::toResumeDto)
                .orElseGet(() -> new FreelancerResumeResponseDto(
                        Collections.emptyList(), Collections.emptyList(), Collections.emptyList()));
    }

    // ─── 이력서 기본정보 수정 ──────────────────────────────────────

    @Transactional
    public void updateResumeBasicInfo(Long freelancerId, ResumeBasicInfoRequestDto request) {
        Resume resume = findByFreelancerIdOrThrow(freelancerId);
        resume.updateBasicInfo(request.name(), request.birthDate(), request.phone(),
                request.email(), request.address());
    }

    // ─── 학력 CUD ──────────────────────────────────────────────────

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

    // ─── 경력 CUD ──────────────────────────────────────────────────

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

    // ─── 자격증 CUD ────────────────────────────────────────────────

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

    // ─── 내부 변환 헬퍼 ──────────────────────────────────────────────

    private Education toEducationEntity(EducationRequestDto e) {
        EduStatus status = null;
        if (e.eduStatus() != null && !e.eduStatus().isBlank()) {
            try {
                status = EduStatus.valueOf(e.eduStatus().toUpperCase());
            } catch (IllegalArgumentException ex) {
                throw new IllegalArgumentException("유효하지 않은 학력 상태 값입니다: " + e.eduStatus());
            }
        }
        return new Education(e.schoolType(), e.schoolName(), e.major(), status, e.entranceDate(), e.graduationDate());
    }

    private Career toCareerEntity(CareerRequestDto c) {
        return new Career(c.companyName(), c.department(), c.position(),
                c.jobType(), c.employmentType(), c.startDate(), c.endDate(), c.description());
    }

    private Certification toCertificationEntity(CertificationRequestDto cert) {
        return new Certification(cert.name(), cert.issuer(), cert.acquisitionDate());
    }

    private Resume findByFreelancerIdOrThrow(Long freelancerId) {
        return resumeRepository.findByFreelancerId(freelancerId)
                .orElseThrow(() -> new IllegalArgumentException("이력서를 찾을 수 없습니다."));
    }

    private FreelancerResumeResponseDto toResumeDto(Resume resume) {
        List<EducationDto> educations = resume.getEducations() == null ? Collections.emptyList() :
                resume.getEducations().stream()
                        .map(e -> new EducationDto(null, e.getSchoolName(), e.getMajor(),
                                e.getEntranceDate() != null ? e.getEntranceDate().toString() : null,
                                e.getGraduationDate() != null ? e.getGraduationDate().toString() : null,
                                e.getEduStatus() != null ? e.getEduStatus().name() : null))
                        .toList();

        List<CareerDto> careers = resume.getCareers() == null ? Collections.emptyList() :
                resume.getCareers().stream()
                        .map(c -> new CareerDto(null, c.getCompanyName(), c.getPosition(),
                                c.getStartDate() != null ? c.getStartDate().toString() : null,
                                c.getEndDate() != null ? c.getEndDate().toString() : null,
                                c.getDescription()))
                        .toList();

        List<CertificationDto> certifications = resume.getCertifications() == null ? Collections.emptyList() :
                resume.getCertifications().stream()
                        .map(cert -> new CertificationDto(null, cert.getName(), cert.getIssuer(),
                                cert.getAcquisitionDate() != null ? cert.getAcquisitionDate().toString() : null))
                        .toList();

        return new FreelancerResumeResponseDto(educations, careers, certifications);
    }
}
