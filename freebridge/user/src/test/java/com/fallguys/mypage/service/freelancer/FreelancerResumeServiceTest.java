package com.fallguys.mypage.service.freelancer;

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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class FreelancerResumeServiceTest {

    @Mock
    private ResumeRepository resumeRepository;

    @InjectMocks
    private FreelancerResumeService freelancerResumeService;

    // ─── getResume ────────────────────────────────────────────────

    @Test
    @DisplayName("[TDD] 1. 이력서 조회: 이력서가 존재하면 학력/경력/자격증이 올바르게 매핑된다")
    void getResume_Success() {
        Long freelancerId = 1L;
        Resume mockResume = new Resume(freelancerId);
        mockResume.update("홍길동", LocalDate.of(1998, 5, 20), "010-1234-5678", "hong@email.com", "서울시",
                List.of(new Education("4년제", "한국대", "컴공", EduStatus.GRADUATED,
                        LocalDate.of(2018, 3, 1), LocalDate.of(2022, 2, 28))),
                List.of(new Career("ABC Corp", "개발팀", "백엔드 개발자", "IT", "정규직",
                        LocalDate.of(2022, 3, 1), null, "Spring Bot 개발")),
                List.of(new Certification("정보처리기사", "한국산업인력공단", LocalDate.of(2021, 11, 15))));

        given(resumeRepository.findByFreelancerId(freelancerId)).willReturn(Optional.of(mockResume));

        FreelancerResumeResponseDto result = freelancerResumeService.getResume(freelancerId);

        assertThat(result.educations()).hasSize(1);
        assertThat(result.educations().get(0).schoolName()).isEqualTo("한국대");
        assertThat(result.careers()).hasSize(1);
        assertThat(result.certifications()).hasSize(1);
    }

    @Test
    @DisplayName("[TDD] 2. 이력서 조회: 이력서가 없으면 빈 리스트 DTO를 반환한다")
    void getResume_NotFound_ReturnsEmpty() {
        given(resumeRepository.findByFreelancerId(2L)).willReturn(Optional.empty());

        FreelancerResumeResponseDto result = freelancerResumeService.getResume(2L);

        assertThat(result.educations()).isEmpty();
        assertThat(result.careers()).isEmpty();
        assertThat(result.certifications()).isEmpty();
    }

    // ─── updateResumeBasicInfo ───────────────────────────────────

    @Test
    @DisplayName("[TDD] 3. 기본정보 수정: 이력서가 있으면 인적사항이 변경된다")
    void updateBasicInfo_Success() {
        Long freelancerId = 10L;
        Resume resume = new Resume(freelancerId);
        given(resumeRepository.findByFreelancerId(freelancerId)).willReturn(Optional.of(resume));

        ResumeBasicInfoRequestDto request = new ResumeBasicInfoRequestDto(
                "수정된이름", LocalDate.of(1995, 6, 15), "010-1111-2222", "update@email.com", "인천시");

        freelancerResumeService.updateResumeBasicInfo(freelancerId, request);

        assertThat(resume.getName()).isEqualTo("수정된이름");
        assertThat(resume.getEmail()).isEqualTo("update@email.com");
    }

    @Test
    @DisplayName("[TDD] 4. 기본정보 수정: 이력서가 없으면 IllegalArgumentException을 던진다")
    void updateBasicInfo_NotFound() {
        given(resumeRepository.findByFreelancerId(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> freelancerResumeService.updateResumeBasicInfo(99L,
                new ResumeBasicInfoRequestDto("name", null, null, null, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("이력서를 찾을 수 없습니다");
    }

    // ─── 학력 CUD ───────────────────────────────────────────────

    @Test
    @DisplayName("[TDD] 5. 학력 추가: 정상 요청 시 교육 엔티티가 이력서에 추가된다")
    void addEducation_Success() {
        Long freelancerId = 20L;
        Resume resume = new Resume(freelancerId);
        given(resumeRepository.findByFreelancerId(freelancerId)).willReturn(Optional.of(resume));

        EducationRequestDto request = new EducationRequestDto("4년제", "부산대", "전산학과", "GRADUATED",
                LocalDate.of(2016, 3, 1), LocalDate.of(2020, 2, 28));

        freelancerResumeService.addEducation(freelancerId, request);

        assertThat(resume.getEducations()).hasSize(1);
        assertThat(resume.getEducations().get(0).getSchoolName()).isEqualTo("부산대");
    }

    @Test
    @DisplayName("[TDD] 6. 학력 수정: 올바른 인덱스로 항목이 교체된다")
    void updateEducation_Success() {
        Long freelancerId = 20L;
        Resume resume = new Resume(freelancerId);
        resume.addEducation(new Education("4년제", "부산대", "전산학과", EduStatus.GRADUATED,
                LocalDate.of(2016, 3, 1), LocalDate.of(2020, 2, 28)));
        given(resumeRepository.findByFreelancerId(freelancerId)).willReturn(Optional.of(resume));

        EducationRequestDto updated = new EducationRequestDto("4년제", "서울대", "소프트웨어공학과", "GRADUATED",
                LocalDate.of(2016, 3, 1), LocalDate.of(2020, 2, 28));

        freelancerResumeService.updateEducation(freelancerId, 0, updated);

        assertThat(resume.getEducations().get(0).getSchoolName()).isEqualTo("서울대");
    }

    @Test
    @DisplayName("[TDD] 7. 학력 삭제: 올바른 인덱스의 항목이 삭제된다")
    void deleteEducation_Success() {
        Long freelancerId = 20L;
        Resume resume = new Resume(freelancerId);
        resume.addEducation(new Education("4년제", "부산대", "전산학과", EduStatus.GRADUATED,
                LocalDate.of(2016, 3, 1), LocalDate.of(2020, 2, 28)));
        given(resumeRepository.findByFreelancerId(freelancerId)).willReturn(Optional.of(resume));

        freelancerResumeService.deleteEducation(freelancerId, 0);

        assertThat(resume.getEducations()).isEmpty();
    }

    @Test
    @DisplayName("[TDD] 8. 학력 삭제: 잘못된 인덱스이면 IllegalArgumentException을 던진다")
    void deleteEducation_InvalidIndex() {
        Long freelancerId = 20L;
        Resume resume = new Resume(freelancerId);
        given(resumeRepository.findByFreelancerId(freelancerId)).willReturn(Optional.of(resume));

        assertThatThrownBy(() -> freelancerResumeService.deleteEducation(freelancerId, 5))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("유효하지 않은 학력 인덱스");
    }

    // ─── 경력 CUD ────────────────────────────────────────────────

    @Test
    @DisplayName("[TDD] 9. 경력 추가: 경력 항목이 이력서에 정상적으로 추가된다")
    void addCareer_Success() {
        Long freelancerId = 30L;
        Resume resume = new Resume(freelancerId);
        given(resumeRepository.findByFreelancerId(freelancerId)).willReturn(Optional.of(resume));

        CareerRequestDto request = new CareerRequestDto("Z Corp", "서버팀", "서버 개발자", "IT", "정규직",
                LocalDate.of(2020, 3, 1), null, "서버 개발");

        freelancerResumeService.addCareer(freelancerId, request);

        assertThat(resume.getCareers()).hasSize(1);
        assertThat(resume.getCareers().get(0).getCompanyName()).isEqualTo("Z Corp");
    }

    // ─── 자격증 CUD ───────────────────────────────────────────────

    @Test
    @DisplayName("[TDD] 10. 자격증 추가: 자격증 항목이 이력서에 정상적으로 추가된다")
    void addCertification_Success() {
        Long freelancerId = 40L;
        Resume resume = new Resume(freelancerId);
        given(resumeRepository.findByFreelancerId(freelancerId)).willReturn(Optional.of(resume));

        CertificationRequestDto request = new CertificationRequestDto("SQLD", "한국데이터산업진흥원",
                LocalDate.of(2021, 5, 10));

        freelancerResumeService.addCertification(freelancerId, request);

        assertThat(resume.getCertifications()).hasSize(1);
        assertThat(resume.getCertifications().get(0).getName()).isEqualTo("SQLD");
    }

    @Test
    @DisplayName("[TDD] 11. 학력 추가 - 잘못된 EduStatus: 유효하지 않은 eduStatus 문자열이면 IllegalArgumentException을 던진다")
    void addEducation_InvalidEduStatus() {
        Long freelancerId = 50L;
        Resume resume = new Resume(freelancerId);
        given(resumeRepository.findByFreelancerId(freelancerId)).willReturn(Optional.of(resume));

        EducationRequestDto request = new EducationRequestDto("4년제", "대학교", "학과", "INVALID_STATUS",
                LocalDate.of(2020, 3, 1), null);

        assertThatThrownBy(() -> freelancerResumeService.addEducation(freelancerId, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("유효하지 않은 학력 상태 값입니다");
    }
}
