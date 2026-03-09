package com.fallguys.mypage.service.freelancer;

import com.fallguys.mypage.api.web.dto.freelancer.request.GradeCalculationRequestDto;
import com.fallguys.mypage.api.web.dto.freelancer.request.QualificationType;
import com.fallguys.mypage.api.web.dto.freelancer.response.GradeCalculationResultDto;
import com.fallguys.mypage.entity.freelancer.AcademicDegree;
import com.fallguys.mypage.entity.freelancer.FreelancerGrade;
import com.fallguys.mypage.entity.freelancer.LicenseGrade;
import com.fallguys.mypage.repository.freelancer.FreelancerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class FreelancerGradeCalculatorService {

    private final FreelancerRepository freelancerRepository;

    /**
     * 등급 산정 계산 (저장 없이 결과만 반환)
     */
    public GradeCalculationResultDto calculate(GradeCalculationRequestDto request) {
        validateRequest(request);

        return switch (request.qualificationType()) {
            case ACADEMIC_CAREER -> calculateByAcademic(request);
            case LICENSED        -> calculateByLicense(request);
        };
    }

    /**
     * 등급 산정 후 Freelancer 엔티티에 등급 반영 (저장)
     */
    @Transactional
    public GradeCalculationResultDto calculateAndSave(Long userId, GradeCalculationRequestDto request) {
        GradeCalculationResultDto result = calculate(request);
        com.fallguys.mypage.entity.freelancer.Freelancer freelancer = freelancerRepository.findByUserId(userId)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("해당 유저의 프리랜서 프로필을 찾을 수 없습니다. userId: " + userId));
        freelancer.changeGrade(result.grade());
        log.info("등급 산정 완료 및 저장 - userId: {}, grade: {}", userId, result.grade());
        return result;
    }

    // ─── 학경력자 등급 계산 ────────────────────────────────────────

    private GradeCalculationResultDto calculateByAcademic(GradeCalculationRequestDto request) {
        if (request.degree() == null) {
            throw new IllegalArgumentException("학경력자 등급 산정 시 최종학력을 입력해야 합니다.");
        }
        int years = safeYears(request.careerYears());
        AcademicDegree degree = request.degree();

        FreelancerGrade grade = switch (degree) {
            case DOCTOR    -> gradeByThresholds(years, 2,  4,  7);
            case MASTER    -> gradeByThresholds(years, 4,  7, 10);
            case BACHELOR  -> gradeByThresholds(years, 6,  9, 12);
            case ASSOCIATE -> gradeByThresholds(years, 8, 11, 14);
        };

        String basis = String.format("%s + 경력 %d년 → %s(%s)",
                degree.getDescription(), years, gradeLabel(grade), grade.name());
        return GradeCalculationResultDto.of(grade, basis, years, "학경력자");
    }

    // ─── 자격자 등급 계산 ───────────────────────────────────────────

    private GradeCalculationResultDto calculateByLicense(GradeCalculationRequestDto request) {
        if (request.licenseGrade() == null) {
            throw new IllegalArgumentException("자격자 등급 산정 시 자격증 구분을 입력해야 합니다.");
        }
        int years = safeYears(request.careerYears());
        LicenseGrade license = request.licenseGrade();

        FreelancerGrade grade = switch (license) {
            case ENGINEER_PROFESSIONAL -> gradeByThresholds(years, 2,  5,  9);
            case ENGINEER              -> gradeByThresholds(years, 4,  7, 11);
            case INDUSTRIAL_ENGINEER   -> gradeByThresholds(years, 6, 10, 14);
            case TECHNICIAN            -> gradeByThresholds(years, 8, 12, 17);
        };

        String basis = String.format("%s + 경력 %d년 → %s(%s)",
                license.getDescription(), years, gradeLabel(grade), grade.name());
        return GradeCalculationResultDto.of(grade, basis, years, "자격자");
    }

    // ─── 공통 헬퍼 ────────────────────────────────────────────────

    /**
     * 경력 연수에 따라 등급을 반환합니다.
     * @param years         경력 연수
     * @param midThreshold  중급 기준 연수 이상
     * @param seniorThreshold 고급 기준 연수
     * @param masterThreshold 특급 기준 연수
     */
    private FreelancerGrade gradeByThresholds(int years, int midThreshold, int seniorThreshold, int masterThreshold) {
        if (years >= masterThreshold) return FreelancerGrade.MASTER;
        if (years >= seniorThreshold) return FreelancerGrade.SENIOR;
        if (years >= midThreshold)    return FreelancerGrade.INTERMEDIATE;
        return FreelancerGrade.JUNIOR;
    }

    private int safeYears(Integer careerYears) {
        if (careerYears == null || careerYears < 0) return 0;
        return careerYears;
    }

    private String gradeLabel(FreelancerGrade grade) {
        return switch (grade) {
            case JUNIOR       -> "초급";
            case INTERMEDIATE -> "중급";
            case SENIOR       -> "고급";
            case MASTER       -> "특급";
        };
    }

    private void validateRequest(GradeCalculationRequestDto request) {
        if (request == null) throw new IllegalArgumentException("등급 산정 요청이 없습니다.");
        if (request.qualificationType() == null) throw new IllegalArgumentException("등급 산정 방식(학경력자/자격자)을 선택해야 합니다.");
    }
}
