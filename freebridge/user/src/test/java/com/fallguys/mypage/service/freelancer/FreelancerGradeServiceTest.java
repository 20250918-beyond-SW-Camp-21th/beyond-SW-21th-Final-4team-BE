package com.fallguys.mypage.service.freelancer;

import com.fallguys.common.exception.BusinessException;
import com.fallguys.common.exception.ErrorCode;
import com.fallguys.mypage.api.web.dto.freelancer.request.GradeSaveRequestDto;
import com.fallguys.mypage.entity.freelancer.Freelancer;
import com.fallguys.mypage.entity.freelancer.FreelancerGrade;
import com.fallguys.mypage.repository.freelancer.FreelancerRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class FreelancerGradeServiceTest {

    @Mock
    private FreelancerRepository freelancerRepository;

    @InjectMocks
    private FreelancerGradeService freelancerGradeService;

    @Test
    @DisplayName("[TDD] 1. education 타입 저장: 학력/등급 파싱 후 등급이 저장된다")
    void saveGrade_Education_Success() {
        Long userId = 1L;
        Freelancer freelancer = Freelancer.create(userId, "백엔드", FreelancerGrade.JUNIOR);
        given(freelancerRepository.findByUserId(userId)).willReturn(Optional.of(freelancer));

        GradeSaveRequestDto request = new GradeSaveRequestDto(
                "education",
                "학사",
                null,
                5,
                "중급"
        );

        freelancerGradeService.saveGrade(userId, request);

        assertThat(freelancer.getGrade()).isEqualTo(FreelancerGrade.INTERMEDIATE);
    }

    @Test
    @DisplayName("[TDD] 2. certification 타입 저장: 자격/등급 파싱 후 등급이 저장된다")
    void saveGrade_Certification_Success() {
        Long userId = 2L;
        Freelancer freelancer = Freelancer.create(userId, "프론트", FreelancerGrade.JUNIOR);
        given(freelancerRepository.findByUserId(userId)).willReturn(Optional.of(freelancer));

        GradeSaveRequestDto request = new GradeSaveRequestDto(
                "certification",
                null,
                "기사",
                7,
                "고급"
        );

        freelancerGradeService.saveGrade(userId, request);

        assertThat(freelancer.getGrade()).isEqualTo(FreelancerGrade.SENIOR);
    }

    @Test
    @DisplayName("[TDD] 3. 존재하지 않는 userId면 USER_NOT_FOUND 예외 발생")
    void saveGrade_UserNotFound() {
        Long userId = 999L;
        given(freelancerRepository.findByUserId(userId)).willReturn(Optional.empty());

        GradeSaveRequestDto request = new GradeSaveRequestDto(
                "education",
                "학사",
                null,
                3,
                "초급"
        );

        assertThatThrownBy(() -> freelancerGradeService.saveGrade(userId, request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.USER_NOT_FOUND);
    }

    @Test
    @DisplayName("[TDD] 4. type이 올바르지 않으면 INVALID_INPUT_VALUE")
    void saveGrade_InvalidType() {
        Long userId = 1L;
        GradeSaveRequestDto request = new GradeSaveRequestDto(
                "other",
                "학사",
                null,
                3,
                "초급"
        );

        assertThatThrownBy(() -> freelancerGradeService.saveGrade(userId, request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_INPUT_VALUE);
    }

    @Test
    @DisplayName("[TDD] 5. education 타입인데 학력값이 없으면 INVALID_INPUT_VALUE")
    void saveGrade_MissingEducation() {
        Long userId = 1L;
        GradeSaveRequestDto request = new GradeSaveRequestDto(
                "education",
                null,
                null,
                3,
                "초급"
        );

        assertThatThrownBy(() -> freelancerGradeService.saveGrade(userId, request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_INPUT_VALUE);
    }

    @Test
    @DisplayName("[TDD] 6. certification 타입인데 자격값이 없으면 INVALID_INPUT_VALUE")
    void saveGrade_MissingCertification() {
        Long userId = 1L;
        GradeSaveRequestDto request = new GradeSaveRequestDto(
                "certification",
                null,
                null,
                3,
                "초급"
        );

        assertThatThrownBy(() -> freelancerGradeService.saveGrade(userId, request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_INPUT_VALUE);
    }

    @Test
    @DisplayName("[TDD] 7. 등급 값이 잘못되면 INVALID_INPUT_VALUE")
    void saveGrade_InvalidGrade() {
        Long userId = 1L;
        GradeSaveRequestDto request = new GradeSaveRequestDto(
                "education",
                "학사",
                null,
                3,
                "최고"
        );

        assertThatThrownBy(() -> freelancerGradeService.saveGrade(userId, request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_INPUT_VALUE);
    }
}