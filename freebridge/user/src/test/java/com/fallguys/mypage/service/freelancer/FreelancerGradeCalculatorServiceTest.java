package com.fallguys.mypage.service.freelancer;

import com.fallguys.mypage.api.web.dto.freelancer.request.GradeCalculationRequestDto;
import com.fallguys.mypage.api.web.dto.freelancer.request.QualificationType;
import com.fallguys.mypage.api.web.dto.freelancer.response.GradeCalculationResultDto;
import com.fallguys.mypage.entity.freelancer.AcademicDegree;
import com.fallguys.mypage.entity.freelancer.FreelancerGrade;
import com.fallguys.mypage.entity.freelancer.LicenseGrade;
import com.fallguys.mypage.repository.freelancer.FreelancerRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class FreelancerGradeCalculatorServiceTest {

    @Mock
    private FreelancerRepository freelancerRepository;

    @InjectMocks
    private FreelancerGradeCalculatorService gradeCalculatorService;

    // ─── 학경력자: 박사 ───────────────────────────────────────────────

    @Test
    @DisplayName("[TDD] 1. 학경력자-박사-0년: 초급(JUNIOR)")
    void academic_Doctor_0year_Junior() {
        GradeCalculationRequestDto req = new GradeCalculationRequestDto(
                QualificationType.ACADEMIC_CAREER, AcademicDegree.DOCTOR, null, 0);
        GradeCalculationResultDto result = gradeCalculatorService.calculate(req);
        assertThat(result.grade()).isEqualTo(FreelancerGrade.JUNIOR);
        assertThat(result.gradeDescription()).isEqualTo("초급");
    }

    @Test
    @DisplayName("[TDD] 2. 학경력자-박사-4년: 고급(SENIOR)")
    void academic_Doctor_4year_Senior() {
        GradeCalculationRequestDto req = new GradeCalculationRequestDto(
                QualificationType.ACADEMIC_CAREER, AcademicDegree.DOCTOR, null, 4);
        assertThat(gradeCalculatorService.calculate(req).grade()).isEqualTo(FreelancerGrade.SENIOR);
    }

    @Test
    @DisplayName("[TDD] 3. 학경력자-박사-7년이상: 특급(MASTER)")
    void academic_Doctor_7year_Master() {
        GradeCalculationRequestDto req = new GradeCalculationRequestDto(
                QualificationType.ACADEMIC_CAREER, AcademicDegree.DOCTOR, null, 7);
        assertThat(gradeCalculatorService.calculate(req).grade()).isEqualTo(FreelancerGrade.MASTER);
    }

    // ─── 학경력자: 학사 ───────────────────────────────────────────────

    @Test
    @DisplayName("[TDD] 4. 학경력자-학사-5년: 초급(JUNIOR)")
    void academic_Bachelor_5year_Junior() {
        GradeCalculationRequestDto req = new GradeCalculationRequestDto(
                QualificationType.ACADEMIC_CAREER, AcademicDegree.BACHELOR, null, 5);
        assertThat(gradeCalculatorService.calculate(req).grade()).isEqualTo(FreelancerGrade.JUNIOR);
    }

    @Test
    @DisplayName("[TDD] 5. 학경력자-학사-9년: 고급(SENIOR)")
    void academic_Bachelor_9year_Senior() {
        GradeCalculationRequestDto req = new GradeCalculationRequestDto(
                QualificationType.ACADEMIC_CAREER, AcademicDegree.BACHELOR, null, 9);
        assertThat(gradeCalculatorService.calculate(req).grade()).isEqualTo(FreelancerGrade.SENIOR);
    }

    @Test
    @DisplayName("[TDD] 6. 학경력자-학사-12년이상: 특급(MASTER)")
    void academic_Bachelor_12year_Master() {
        GradeCalculationRequestDto req = new GradeCalculationRequestDto(
                QualificationType.ACADEMIC_CAREER, AcademicDegree.BACHELOR, null, 15);
        assertThat(gradeCalculatorService.calculate(req).grade()).isEqualTo(FreelancerGrade.MASTER);
    }

    // ─── 자격자: 기사 ─────────────────────────────────────────────────

    @Test
    @DisplayName("[TDD] 7. 자격자-기사-0년: 초급(JUNIOR)")
    void license_Engineer_0year_Junior() {
        GradeCalculationRequestDto req = new GradeCalculationRequestDto(
                QualificationType.LICENSED, null, LicenseGrade.ENGINEER, 0);
        assertThat(gradeCalculatorService.calculate(req).grade()).isEqualTo(FreelancerGrade.JUNIOR);
    }

    @Test
    @DisplayName("[TDD] 8. 자격자-기사-7년: 고급(SENIOR)")
    void license_Engineer_7year_Senior() {
        GradeCalculationRequestDto req = new GradeCalculationRequestDto(
                QualificationType.LICENSED, null, LicenseGrade.ENGINEER, 7);
        assertThat(gradeCalculatorService.calculate(req).grade()).isEqualTo(FreelancerGrade.SENIOR);
    }

    @Test
    @DisplayName("[TDD] 9. 자격자-기술사-9년이상: 특급(MASTER)")
    void license_EngineerProfessional_9year_Master() {
        GradeCalculationRequestDto req = new GradeCalculationRequestDto(
                QualificationType.LICENSED, null, LicenseGrade.ENGINEER_PROFESSIONAL, 9);
        assertThat(gradeCalculatorService.calculate(req).grade()).isEqualTo(FreelancerGrade.MASTER);
    }

    // ─── 경계값/예외 ─────────────────────────────────────────────────

    @Test
    @DisplayName("[TDD] 10. 경력 연수가 null인 경우 0으로 처리하여 초급이 된다")
    void nullCareerYears_TreatedAsZero() {
        GradeCalculationRequestDto req = new GradeCalculationRequestDto(
                QualificationType.ACADEMIC_CAREER, AcademicDegree.BACHELOR, null, null);
        assertThat(gradeCalculatorService.calculate(req).grade()).isEqualTo(FreelancerGrade.JUNIOR);
    }

    @Test
    @DisplayName("[TDD] 11. 학경력자 선택 시 degree가 null이면 IllegalArgumentException")
    void academic_NullDegree_ThrowsException() {
        GradeCalculationRequestDto req = new GradeCalculationRequestDto(
                QualificationType.ACADEMIC_CAREER, null, null, 5);
        assertThatThrownBy(() -> gradeCalculatorService.calculate(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("최종학력을 입력해야 합니다");
    }

    @Test
    @DisplayName("[TDD] 12. 자격자 선택 시 licenseGrade가 null이면 IllegalArgumentException")
    void license_NullLicenseGrade_ThrowsException() {
        GradeCalculationRequestDto req = new GradeCalculationRequestDto(
                QualificationType.LICENSED, null, null, 5);
        assertThatThrownBy(() -> gradeCalculatorService.calculate(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("자격증 구분을 입력해야 합니다");
    }

    @Test
    @DisplayName("[TDD] 13. qualificationType이 null이면 IllegalArgumentException")
    void nullQualificationType_ThrowsException() {
        GradeCalculationRequestDto req = new GradeCalculationRequestDto(null, null, null, 5);
        assertThatThrownBy(() -> gradeCalculatorService.calculate(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("등급 산정 방식");
    }

    @Test
    @DisplayName("[TDD] 14. 전문학사-14년: 특급(MASTER) - 전문학사 특급 기준 확인")
    void academic_Associate_14year_Master() {
        GradeCalculationRequestDto req = new GradeCalculationRequestDto(
                QualificationType.ACADEMIC_CAREER, AcademicDegree.ASSOCIATE, null, 14);
        assertThat(gradeCalculatorService.calculate(req).grade()).isEqualTo(FreelancerGrade.MASTER);
    }
}
