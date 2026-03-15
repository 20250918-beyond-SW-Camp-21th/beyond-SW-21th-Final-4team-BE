package com.fallguys.mypage.service.freelancer;

import com.fallguys.common.exception.BusinessException;
import com.fallguys.common.exception.ErrorCode;
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

    public GradeCalculationResultDto calculate(GradeCalculationRequestDto request) {
        validateRequest(request);

        return switch (request.qualificationType()) {
            case ACADEMIC_CAREER -> calculateByAcademic(request);
            case LICENSED -> calculateByLicense(request);
        };
    }

    @Transactional
    public GradeCalculationResultDto calculateAndSave(Long userId, GradeCalculationRequestDto request) {
        GradeCalculationResultDto result = calculate(request);
        com.fallguys.mypage.entity.freelancer.Freelancer freelancer = freelancerRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        freelancer.changeGrade(result.grade());
        log.info("등급 산정 완료 및 저장 - userId: {}, grade: {}", userId, result.grade());
        return result;
    }

    private GradeCalculationResultDto calculateByAcademic(GradeCalculationRequestDto request) {
        if (request.degree() == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
        int years = safeYears(request.careerYears());
        AcademicDegree degree = request.degree();

        FreelancerGrade grade = switch (degree) {
            case DOCTOR -> gradeByThresholds(years, 2, 4, 7);
            case MASTER -> gradeByThresholds(years, 4, 7, 10);
            case BACHELOR -> gradeByThresholds(years, 6, 9, 12);
            case ASSOCIATE -> gradeByThresholds(years, 8, 11, 14);
        };

        String basis = String.format("%s + 경력 %d년 -> %s(%s)",
                degree.getDescription(), years, gradeLabel(grade), grade.name());
        return GradeCalculationResultDto.of(grade, basis, years, "학경력자");
    }

    private GradeCalculationResultDto calculateByLicense(GradeCalculationRequestDto request) {
        if (request.licenseGrade() == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
        int years = safeYears(request.careerYears());
        LicenseGrade license = request.licenseGrade();

        FreelancerGrade grade = switch (license) {
            case ENGINEER_PROFESSIONAL -> gradeByThresholds(years, 2, 5, 9);
            case ENGINEER -> gradeByThresholds(years, 4, 7, 11);
            case INDUSTRIAL_ENGINEER -> gradeByThresholds(years, 6, 10, 14);
            case TECHNICIAN -> gradeByThresholds(years, 8, 12, 17);
        };

        String basis = String.format("%s + 경력 %d년 -> %s(%s)",
                license.getDescription(), years, gradeLabel(grade), grade.name());
        return GradeCalculationResultDto.of(grade, basis, years, "자격자");
    }

    private FreelancerGrade gradeByThresholds(int years, int midThreshold, int seniorThreshold, int masterThreshold) {
        if (years >= masterThreshold) return FreelancerGrade.MASTER;
        if (years >= seniorThreshold) return FreelancerGrade.SENIOR;
        if (years >= midThreshold) return FreelancerGrade.INTERMEDIATE;
        return FreelancerGrade.JUNIOR;
    }

    private int safeYears(Integer careerYears) {
        if (careerYears == null || careerYears < 0) return 0;
        return careerYears;
    }

    private String gradeLabel(FreelancerGrade grade) {
        return switch (grade) {
            case JUNIOR -> "초급";
            case INTERMEDIATE -> "중급";
            case SENIOR -> "고급";
            case MASTER -> "특급";
        };
    }

    private void validateRequest(GradeCalculationRequestDto request) {
        if (request == null) throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        if (request.qualificationType() == null) throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
    }
}
