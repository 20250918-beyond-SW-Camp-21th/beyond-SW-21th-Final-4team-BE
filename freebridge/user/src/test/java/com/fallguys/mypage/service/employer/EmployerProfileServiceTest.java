package com.fallguys.mypage.service.employer;

import com.fallguys.mypage.api.web.dto.employer.response.EmployerBasicProfileDto;
import com.fallguys.mypage.entity.employer.Employer;
import com.fallguys.mypage.entity.employer.Scale;
import com.fallguys.mypage.entity.employer.Subscription;
import com.fallguys.mypage.repository.employer.EmployerRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.Assertions;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class EmployerProfileServiceTest {

    @InjectMocks
    private EmployerProfileService employerProfileService;

    @Mock
    private EmployerRepository employerRepository;

    @Mock
    private com.fallguys.common.port.FileStorage fileStorage;

    @Test
    @DisplayName("[TDD] 1. 고용주 프로필 조회 시 Repository 호출 여부 검증")
    void verify_repository_interaction_when_fetching_profile() {
        // given
        Long userId = 100L;
        Employer mockEmployer = Employer.create(userId, Subscription.BASIC, "Test Co", Scale.S1_4);
        given(employerRepository.findByUserId(userId)).willReturn(Optional.of(mockEmployer));

        // when
        employerProfileService.getProfile(userId);

        // then
        // findByUserId가 정확히 userId 파라미터로 1번 호출되었는지 검증 (TDD 방식의 행위 검증)
        org.mockito.Mockito.verify(employerRepository, org.mockito.Mockito.times(1)).findByUserId(userId);
    }

    @Test
    @DisplayName("[TDD] 2. 고용주 프로필 반환 시 필수 DTO 매핑 데이터 검증")
    void verify_dto_mapping_with_mock_data() {
        // given
        Long userId = 200L;
        Employer mockEmployer = Employer.create(
                userId,
                Subscription.PRO,
                "TDD Company",
                Scale.S10_29
        );

        mockEmployer.updateProfile(
                "TDD Company",
                "IT/Platform",
                Scale.S10_29,
                "Pangyo",
                "https://tdd.example.com",
                "TDD Driven Company",
                "logo_tdd.png"
        );

        given(employerRepository.findByUserId(userId)).willReturn(Optional.of(mockEmployer));

        // when
        EmployerBasicProfileDto result = employerProfileService.getProfile(userId);

        // then
        // TDD 과정에서 필수적으로 매핑되어야 할 핵심 필드들 중심의 상태 검증
        assertThat(result).isNotNull();
        assertThat(result.companyName()).isEqualTo("TDD Company");
        assertThat(result.industry()).isEqualTo("IT/Platform");
        assertThat(result.scale()).isEqualTo(Scale.S10_29.name());
        assertThat(result.location()).isEqualTo("Pangyo");
        assertThat(result.status()).isEqualTo("POTENTIAL"); // 초기 가입 상태 확인
    }

    @Test
    @DisplayName("[TDD] 3. 고용주 프로필 업데이트 시 Repository 조회 및 데이터 변경 검증")
    void verify_profile_update_mapping_and_interaction() {
        // given
        Long userId = 300L;
        Employer mockEmployer = Employer.create(
                userId,
                Subscription.BASIC,
                "Old Company",
                Scale.S1_4
        );
        given(employerRepository.findByUserId(userId)).willReturn(Optional.of(mockEmployer));

        com.fallguys.mypage.api.web.dto.employer.request.EmployerProfileUpdateRequestDto requestDto =
                new com.fallguys.mypage.api.web.dto.employer.request.EmployerProfileUpdateRequestDto(
                        "New Company",
                        "FinTech",
                        "S30_99",
                        "Yeouido",
                        "https://new.com",
                        "Updated Description",
                        "0108998932"
                );

        // when
        employerProfileService.updateProfile(userId, requestDto);

        // then
        // 1. findByUserId 호출 검증
        org.mockito.Mockito.verify(employerRepository, org.mockito.Mockito.times(1)).findByUserId(userId);

        // 2. Entity 내부의 필드가 RequestDto 값으로 정상 편경되었는지 상태 검증 (더티 체킹 발생 여부 테스트)
        assertThat(mockEmployer.getCompanyName()).isEqualTo("New Company");
        assertThat(mockEmployer.getIndustry()).isEqualTo("FinTech");
        assertThat(mockEmployer.getScale().name()).isEqualTo("S30_99");
        assertThat(mockEmployer.getLocation()).isEqualTo("Yeouido");
        assertThat(mockEmployer.getWebsiteUrl()).isEqualTo("https://new.com");
        assertThat(mockEmployer.getDescription()).isEqualTo("Updated Description");
    }

    @Test
    @DisplayName("[TDD] 3-1. 고용주 프로필 업데이트 시 잘못된 Scale 값은 IllegalArgumentException 예외를 발생시킨다")
    void verify_scale_parsing_failure_throws_exception() {
        // given
        Long userId = 300L;
        Employer mockEmployer = Employer.create(userId, Subscription.BASIC, "Old Company", Scale.S1_4);
        given(employerRepository.findByUserId(userId)).willReturn(Optional.of(mockEmployer));

        com.fallguys.mypage.api.web.dto.employer.request.EmployerProfileUpdateRequestDto requestDto =
                new com.fallguys.mypage.api.web.dto.employer.request.EmployerProfileUpdateRequestDto(
                        "New Company",
                        "FinTech",
                        "INVALID_SCALE",
                        "Yeouido",
                        "https://new.com",
                        "Updated Description",
                        "0108998932"
                );

        // when & then
        IllegalArgumentException exception = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> employerProfileService.updateProfile(userId, requestDto)
        );
        assertThat(exception.getMessage()).contains("유효하지 않은 기업 규모(Scale) 값입니다: INVALID_SCALE");
    }

    @Test
    @DisplayName("[TDD] 4. 고용주 로고 이미지 파일 S3 업로드 및 Entity 변경 검증")
    void verify_logo_upload_and_update_interaction() throws java.io.IOException {
        // given
        Long userId = 400L;
        Employer mockEmployer = Employer.create(
                userId,
                Subscription.BASIC,
                "Logo Company",
                Scale.S1_4
        );
        given(employerRepository.findByUserId(userId)).willReturn(Optional.of(mockEmployer));

        // Mock MultipartFile
        byte[] validPngBytes = new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
        org.springframework.mock.web.MockMultipartFile mockFile =
                new org.springframework.mock.web.MockMultipartFile("file", "logo.png", "image/png", validPngBytes);

        // Mock FileStorage (S3 Upload)
        String uploadedS3Url = "employers/logo/mock-uuid.png";
        given(fileStorage.upload(
                org.mockito.ArgumentMatchers.any(byte[].class),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.eq("image/png")
        )).willReturn(uploadedS3Url);

        // when
        String resultUrl = employerProfileService.updateLogoUrl(userId, mockFile);

        // then
        org.mockito.Mockito.verify(employerRepository, org.mockito.Mockito.times(1)).findByUserId(userId);
        org.mockito.Mockito.verify(fileStorage, org.mockito.Mockito.times(1)).upload(
                org.mockito.ArgumentMatchers.any(byte[].class),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.eq("image/png")
        );

        assertThat(resultUrl).isEqualTo(uploadedS3Url);
        assertThat(mockEmployer.getLogoUrl()).isEqualTo(uploadedS3Url);
    }
    @Test
    @DisplayName("[TDD] 5. 고용주 CRM 알림 조회 시 요금제 기반 업셀링 여부 검증")
    void verify_crm_alerts_based_on_subscription() {
        // given
        Long userId = 500L;
        Employer mockEmployer = Employer.create(
                userId,
                Subscription.BASIC, // BASIC 요금제는 업셀링 대상
                "Alert Company",
                Scale.S1_4
        );
        given(employerRepository.findByUserId(userId)).willReturn(Optional.of(mockEmployer));

        // when
        com.fallguys.mypage.api.web.dto.employer.response.CrmAlertsResponseDto result = 
                employerProfileService.getCrmAlerts(userId);

        // then
        org.mockito.Mockito.verify(employerRepository, org.mockito.Mockito.times(1)).findByUserId(userId);
        assertThat(result).isNotNull();
        assertThat(result.isPremiumUpsellEligible()).isTrue(); // BASIC이므로 true여야 함
    }
}

