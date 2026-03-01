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

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class EmployerProfileServiceTest {

    @InjectMocks
    private EmployerProfileService employerProfileService;

    @Mock
    private EmployerRepository employerRepository;

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
}

