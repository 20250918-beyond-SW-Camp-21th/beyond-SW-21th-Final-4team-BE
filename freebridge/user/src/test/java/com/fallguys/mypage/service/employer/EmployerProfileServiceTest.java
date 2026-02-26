package com.fallguys.mypage.service.employer;

import com.fallguys.mypage.dto.employer.response.EmployerProfileResponseDto;
import com.fallguys.mypage.entity.employer.Employer;
import com.fallguys.mypage.entity.employer.Subscription;
import com.fallguys.mypage.entity.employer.Scale;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmployerProfileServiceTest {

    @InjectMocks
    private EmployerProfileService employerProfileService;

    @Mock
    private com.fallguys.mypage.repository.employer.EmployerRepository employerRepository;
    
    @Mock
    private com.fallguys.mypage.repository.employer.ProjectRepository projectRepository;


    @Test
    @DisplayName("고용주 프로필 조회 정상 케이스 - FREE 요금제이면서 완료된 프로젝트가 1건 이상이면 프리미엄 업셀 알림이 활성화(true) 되어야 한다.")
    void getEmployerProfile_Success_With_Upsell_Flag() {
        // Given
        String userId = "1";
        
        // Mock Employer 데이터 준비: 요금제는 FREE
        Employer mockEmployer = Employer.create(
                1L, 
                Subscription.BASIC,
                "테스트 기업", 
                Scale.S1_4
        );
        
        // Mock Project Repository 응답 세팅: 완료된 프로젝트 개수 1건으로 세팅
        when(employerRepository.findByUserId(Long.parseLong(userId))).thenReturn(java.util.Optional.of(mockEmployer));
        when(projectRepository.countCompletedProjectsByEmployerId(mockEmployer.getEmployerId())).thenReturn(1);

        // When
        EmployerProfileResponseDto response = employerProfileService.getEmployerProfile(userId);


        // Then
        // 데이터가 모킹된 조건에 맞춰 crmAlerts 필드의 isPremiumUpsellEligible 값이 true 인지 검증
        assertThat(response).isNotNull();
        assertThat(response.crmAlerts()).isNotNull();
        assertThat(response.crmAlerts().isPremiumUpsellEligible()).isTrue();
    }
}
