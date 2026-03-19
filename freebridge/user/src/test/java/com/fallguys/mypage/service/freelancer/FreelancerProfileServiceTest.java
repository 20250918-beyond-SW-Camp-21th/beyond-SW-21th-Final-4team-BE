package com.fallguys.mypage.service.freelancer;

import com.fallguys.common.ai.port.RecommendationEngine;
import com.fallguys.common.exception.BusinessException;
import com.fallguys.common.exception.ErrorCode;
import com.fallguys.common.port.FileStorage;
import com.fallguys.mypage.api.shared.SharedMypageApi;
import com.fallguys.mypage.api.web.dto.freelancer.request.FreelancerProfileUpdateRequestDto;
import com.fallguys.mypage.api.web.dto.freelancer.response.FreelancerEvaluationSummaryDto;
import com.fallguys.mypage.api.web.dto.freelancer.response.FreelancerProfileResponseDto;
import com.fallguys.mypage.api.web.dto.freelancer.response.FreelancerProjectStatusStatsDto;
import com.fallguys.mypage.entity.freelancer.Freelancer;
import com.fallguys.mypage.entity.freelancer.FreelancerGrade;
import com.fallguys.mypage.entity.freelancer.WorkConditions;
import com.fallguys.mypage.repository.freelancer.FreelancerRepository;
import com.fallguys.user.api.shared.response.ExternalUserResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class FreelancerProfileServiceTest {

    @InjectMocks
    private FreelancerProfileService freelancerProfileService;

    @Mock
    private FreelancerRepository freelancerRepository;

    @Mock
    private FileStorage fileStorage;

    @Mock
    private SharedMypageApi sharedMypageApi;

    @Mock
    private FreelancerReviewService freelancerReviewService;

    @Mock
    private RecommendationEngine recommendationEngine;

    @Mock
    private FreelancerProjectService freelancerProjectService;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @BeforeEach
    void setUp() {
        TransactionSynchronizationManager.initSynchronization();
        lenient().doNothing().when(freelancerReviewService).syncReviewMetricsToFreelancer(anyLong());
        lenient().when(freelancerReviewService.getReviewSummary(anyLong()))
                .thenReturn(FreelancerEvaluationSummaryDto.empty(null));
        lenient().when(freelancerProjectService.getProjectStats(anyLong()))
                .thenReturn(FreelancerProjectStatusStatsDto.empty());
    }

    @AfterEach
    void tearDown() {
        TransactionSynchronizationManager.clear();
    }

    @Test
    @DisplayName("[TDD] 1. \uD504\uB85C\uD544 \uC870\uD68C: \uC815\uC0C1 \uC751\uB2F5\uC744 \uBC18\uD658\uD55C\uB2E4")
    void getProfile_Success() {
        Long userId = 100L;
        Long freelancerId = 10L;
        LocalDate futureStartDate = LocalDate.now().plusDays(30);

        Freelancer mockFreelancer = Freelancer.create(userId, "Backend Developer", FreelancerGrade.JUNIOR);
        ReflectionTestUtils.setField(mockFreelancer, "freelancerId", freelancerId, Long.class);
        mockFreelancer.updateWorkConditions(new WorkConditions(
                "PERSONAL",
                futureStartDate,
                "REMOTE",
                "SEOUL"
        ));

        ExternalUserResponse userResponse = ExternalUserResponse.builder()
                .id(userId)
                .email("alice@example.com")
                .name("Alice")
                .build();

        given(freelancerRepository.findByUserId(userId)).willReturn(Optional.of(mockFreelancer));
        given(sharedMypageApi.getUserById(userId)).willReturn(userResponse);

        FreelancerProfileResponseDto result = freelancerProfileService.getProfile(userId);

        assertThat(result).isNotNull();
        assertThat(result.basicProfile()).isNotNull();
        assertThat(result.stats()).isNotNull();
        assertThat(result.basicProfile().job()).isEqualTo("Backend Developer");
        assertThat(result.basicProfile().grade()).isEqualTo("JUNIOR");
        assertThat(result.basicProfile().status()).isEqualTo("POTENTIAL");
        assertThat(result.basicProfile().name()).isEqualTo("Alice");
        assertThat(result.basicProfile().email()).isEqualTo("alice@example.com");
        assertThat(result.basicProfile().workConditions()).isNotNull();
        assertThat(result.basicProfile().workConditions().workType()).isEqualTo("PERSONAL");
        assertThat(result.basicProfile().workConditions().workStyle()).isEqualTo("REMOTE");
        assertThat(result.basicProfile().workConditions().workLocation()).isEqualTo("SEOUL");
        assertThat(result.basicProfile().workConditions().availableStartDate()).isEqualTo(futureStartDate);
        assertThat(result.basicProfile().crmAlerts()).isNotNull();
        assertThat(result.basicProfile().crmAlerts().isRateBumpEligible()).isFalse();
        assertThat(result.basicProfile().crmAlerts().isBurnoutWarning()).isFalse();
        assertThat(result.basicProfile().crmAlerts().isChurnWarning()).isFalse();
        assertThat(result.stats().statContact()).isEqualTo(0);
        assertThat(result.stats().statChat()).isEqualTo(0);
        assertThat(result.stats().statContract()).isEqualTo(0);

        verify(freelancerReviewService).syncReviewMetricsToFreelancer(userId);
        verify(freelancerRepository).findByUserId(userId);
        verify(sharedMypageApi).getUserById(userId);
        verify(freelancerReviewService).getReviewSummary(userId);
        verify(freelancerProjectService).getProjectStats(freelancerId);
    }

    @Test
    @DisplayName("[TDD] 2. \uD504\uB85C\uD544 \uC870\uD68C: \uC874\uC7AC\uD558\uC9C0 \uC54A\uB294 userId\uBA74 USER_NOT_FOUND \uC608\uC678 \uBC1C\uC0DD")
    void getProfile_NotFound_ThrowsException() {
        Long userId = 999L;
        given(freelancerRepository.findByUserId(userId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> freelancerProfileService.getProfile(userId))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.USER_NOT_FOUND);
    }

    @Test
    @DisplayName("[TDD] 3. \uD504\uB85C\uD544 \uC218\uC815: \uC815\uC0C1 \uC694\uCCAD \uC2DC \uD504\uB85C\uD544 \uC815\uBCF4\uAC00 \uC5C5\uB370\uC774\uD2B8\uB41C\uB2E4")
    void updateProfile_Success() {
        Long userId = 200L;
        Long freelancerId = 20L;
        Freelancer mockFreelancer = Freelancer.create(userId, "Fullstack Developer", FreelancerGrade.JUNIOR);
        ReflectionTestUtils.setField(mockFreelancer, "freelancerId", freelancerId, Long.class);
        given(freelancerRepository.findByUserId(userId)).willReturn(Optional.of(mockFreelancer));

        FreelancerProfileUpdateRequestDto request = new FreelancerProfileUpdateRequestDto(
                "Platform Engineer",
                "Experienced backend and frontend engineer.",
                5,
                50000L,
                List.of("Java", "React", "Spring"),
                "TEAM",
                LocalDate.now().plusDays(45),
                "HYBRID",
                "BUSAN",
                "Updated Name",
                4,
                5,
                3,
                4,
                4,
                2,
                4.5
        );

        freelancerProfileService.updateProfile(userId, request);

        verify(freelancerRepository).findByUserId(userId);
        verify(sharedMypageApi).updateUserName(userId, "Updated Name");
        verify(redisTemplate).delete("ai:reco:jobs:v3:" + userId);
        verify(redisTemplate, never()).delete("ai:lock:jobs:" + userId);
        verify(recommendationEngine).syncToAiServer(
                eq(freelancerId),
                eq(freelancerId),
                eq("new_profile"),
                anyString(),
                eq("POTENTIAL")
        );

        assertThat(mockFreelancer.getJob()).isEqualTo("Platform Engineer");
        assertThat(mockFreelancer.getIntroduction()).isEqualTo("Experienced backend and frontend engineer.");
        assertThat(mockFreelancer.getCareerYears()).isEqualTo(5);
        assertThat(mockFreelancer.getWage()).isEqualTo(50000L);
        assertThat(mockFreelancer.getSkills()).containsExactlyInAnyOrder("Java", "React", "Spring");
        assertThat(mockFreelancer.getWorkConditions()).isNotNull();
        assertThat(mockFreelancer.getWorkConditions().getConditionsType()).isEqualTo("TEAM");
        assertThat(mockFreelancer.getWorkConditions().getStartDate()).isEqualTo(LocalDate.now().plusDays(45));
        assertThat(mockFreelancer.getWorkConditions().getWorkStyle()).isEqualTo("HYBRID");
        assertThat(mockFreelancer.getWorkConditions().getLocation()).isEqualTo("BUSAN");
        assertThat(mockFreelancer.getExpertise()).isNotNull();
        assertThat(mockFreelancer.getExpertise().getProgramming()).isEqualTo(4);
        assertThat(mockFreelancer.getExpertise().getFramework()).isEqualTo(5);
        assertThat(mockFreelancer.getExpertise().getProblemSolving()).isEqualTo(3);
        assertThat(mockFreelancer.getCollaboration()).isNotNull();
        assertThat(mockFreelancer.getCollaboration().getCommunication()).isEqualTo(4);
        assertThat(mockFreelancer.getCollaboration().getScheduleAdherence()).isEqualTo(4);
        assertThat(mockFreelancer.getCollaboration().getDispute()).isEqualTo(2);
        assertThat(mockFreelancer.getAverageRate()).isEqualTo(4.5);
    }

    @Test
    @DisplayName("[TDD] 4-1. \uD504\uB85C\uD544 \uC218\uC815: \uD504\uB9AC\uB79C\uC11C \uACF5\uACE0 \uCD94\uCC9C \uCE90\uC2DC\uAC00 \uC0AD\uC81C\uB41C\uB2E4")
    void updateProfile_EvictsFreelancerJobRecommendationCache() {
        Long userId = 201L;
        Long freelancerId = 21L;
        Freelancer mockFreelancer = Freelancer.create(userId, "Backend Developer", FreelancerGrade.JUNIOR);
        ReflectionTestUtils.setField(mockFreelancer, "freelancerId", freelancerId, Long.class);
        given(freelancerRepository.findByUserId(userId)).willReturn(Optional.of(mockFreelancer));

        FreelancerProfileUpdateRequestDto request = new FreelancerProfileUpdateRequestDto(
                "Backend Developer",
                "Profile intro",
                3,
                40000L,
                List.of("Java"),
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );

        freelancerProfileService.updateProfile(userId, request);

        verify(redisTemplate).delete("ai:reco:jobs:v3:" + userId);
        verify(redisTemplate, never()).delete("ai:lock:jobs:" + userId);
        verify(recommendationEngine).syncToAiServer(
                eq(freelancerId),
                eq(freelancerId),
                eq("new_profile"),
                anyString(),
                eq("POTENTIAL")
        );
    }

    @Test
    @DisplayName("[TDD] 4-2. \uD504\uB85C\uD544 \uC218\uC815: \uC874\uC7AC\uD558\uC9C0 \uC54A\uB294 userId\uBA74 USER_NOT_FOUND \uC608\uC678 \uBC1C\uC0DD")
    void updateProfile_NotFound_ThrowsException() {
        Long userId = 999L;
        given(freelancerRepository.findByUserId(userId)).willReturn(Optional.empty());

        FreelancerProfileUpdateRequestDto request = new FreelancerProfileUpdateRequestDto(
                "Backend Developer",
                "Intro",
                3,
                40000L,
                List.of("Java"),
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );

        assertThatThrownBy(() -> freelancerProfileService.updateProfile(userId, request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.USER_NOT_FOUND);
    }

    @Test
    @DisplayName("[TDD] 5. \uD504\uB85C\uD544 \uC774\uBBF8\uC9C0 \uC5C5\uB85C\uB4DC: \uC815\uC0C1 \uC694\uCCAD \uC2DC S3 URL\uC744 \uBC18\uD658\uD55C\uB2E4")
    void updateAvatarUrl_Success() throws Exception {
        Long userId = 300L;
        Freelancer mockFreelancer = Freelancer.create(userId, "Designer", FreelancerGrade.SENIOR);
        given(freelancerRepository.findByUserId(userId)).willReturn(Optional.of(mockFreelancer));

        byte[] validPngBytes = new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
        MockMultipartFile mockFile = new MockMultipartFile("file", "avatar.png", "image/png", validPngBytes);

        String uploadedUrl = "avatars/mock-uuid.png";
        given(fileStorage.upload(any(byte[].class), anyString(), eq("image/png"))).willReturn(uploadedUrl);

        String result = freelancerProfileService.updateAvatarUrl(userId, mockFile);

        verify(freelancerRepository).findByUserId(userId);
        verify(fileStorage).upload(any(byte[].class), anyString(), eq("image/png"));
        assertThat(result).isEqualTo(uploadedUrl);
    }

    @Test
    @DisplayName("[TDD] 6. \uD504\uB85C\uD544 \uC774\uBBF8\uC9C0 \uC5C5\uB85C\uB4DC: 5MB \uCD08\uACFC \uD30C\uC77C\uC774\uBA74 INVALID_INPUT_VALUE")
    void updateAvatarUrl_TooLargeFile_ThrowsException() {
        Long userId = 300L;
        byte[] largeFileBytes = new byte[6 * 1024 * 1024];
        MockMultipartFile oversizedFile = new MockMultipartFile("file", "big.png", "image/png", largeFileBytes);

        assertThatThrownBy(() -> freelancerProfileService.updateAvatarUrl(userId, oversizedFile))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_INPUT_VALUE);
    }

    @Test
    @DisplayName("[TDD] 7. \uD504\uB85C\uD544 \uC774\uBBF8\uC9C0 \uC5C5\uB85C\uB4DC: \uC798\uBABB\uB41C MIME \uD0C0\uC785\uC774\uBA74 INVALID_INPUT_VALUE")
    void updateAvatarUrl_InvalidContentType_ThrowsException() {
        Long userId = 300L;
        MockMultipartFile invalidFile = new MockMultipartFile("file", "script.txt", "text/plain", "hello".getBytes());

        assertThatThrownBy(() -> freelancerProfileService.updateAvatarUrl(userId, invalidFile))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_INPUT_VALUE);
    }
}
