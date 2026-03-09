package com.fallguys.mypage.service.freelancer;

import com.fallguys.common.exception.BusinessException;
import com.fallguys.common.exception.ErrorCode;
import com.fallguys.common.port.FileStorage;
import com.fallguys.mypage.api.shared.SharedMypageApi;
import com.fallguys.mypage.api.web.dto.freelancer.request.FreelancerProfileUpdateRequestDto;
import com.fallguys.mypage.api.web.dto.freelancer.response.FreelancerProfileResponseDto;
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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

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

    @BeforeEach
    void setUp() {
        TransactionSynchronizationManager.initSynchronization();
    }

    @AfterEach
    void tearDown() {
        TransactionSynchronizationManager.clear();
    }

    @Test
    @DisplayName("[TDD] 1. 프로필 조회: FreelancerRepository를 통해 정상 조회된다")
    void getProfile_Success() {
        // given
        Long userId = 100L;
        Freelancer mockFreelancer = Freelancer.create(userId, "백엔드 개발자", FreelancerGrade.JUNIOR);
        mockFreelancer.updateWorkConditions(new WorkConditions(
                "개인",
                LocalDate.of(2026, 3, 10),
                "원격",
                "서울"
        ));
        ExternalUserResponse userResponse = ExternalUserResponse.builder()
                .id(userId)
                .email("gdragon@gmail.com")
                .name("지드래곤")
                .build();

        given(freelancerRepository.findByUserId(userId)).willReturn(Optional.of(mockFreelancer));
        given(sharedMypageApi.getUserById(userId)).willReturn(userResponse);

        // when
        FreelancerProfileResponseDto result = freelancerProfileService.getProfile(userId);

        // then
        assertThat(result).isNotNull();
        assertThat(result.basicProfile()).isNotNull();
        assertThat(result.stats()).isNotNull();
        assertThat(result.basicProfile().job()).isEqualTo("백엔드 개발자");
        assertThat(result.basicProfile().grade()).isEqualTo("JUNIOR");
        assertThat(result.basicProfile().status()).isEqualTo("POTENTIAL");
        assertThat(result.basicProfile().name()).isEqualTo("지드래곤");
        assertThat(result.basicProfile().email()).isEqualTo("gdragon@gmail.com");
        assertThat(result.basicProfile().workConditions()).isNotNull();
        assertThat(result.basicProfile().workConditions().workType()).isEqualTo("개인");
        assertThat(result.basicProfile().workConditions().workStyle()).isEqualTo("원격");
        assertThat(result.basicProfile().workConditions().workLocation()).isEqualTo("서울");
        assertThat(result.basicProfile().workConditions().availableStartDate())
                .isEqualTo(LocalDate.of(2026, 3, 10));
        assertThat(result.stats().statContact()).isEqualTo(0);
        assertThat(result.stats().statChat()).isEqualTo(0);
        assertThat(result.stats().statContract()).isEqualTo(0);

        verify(freelancerRepository, times(1)).findByUserId(userId);
        verify(sharedMypageApi, times(1)).getUserById(userId);
    }

    @Test
    @DisplayName("[TDD] 2. 프로필 조회: 존재하지 않는 userId면 USER_NOT_FOUND 예외 발생")
    void getProfile_NotFound_ThrowsException() {
        // given
        Long userId = 999L;
        given(freelancerRepository.findByUserId(userId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> freelancerProfileService.getProfile(userId))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.USER_NOT_FOUND);
    }

    @Test
    @DisplayName("[TDD] 3. 프로필 수정: 정상 요청 시 기본/근무조건 정보가 업데이트된다")
    void updateProfile_Success() {
        // given
        Long userId = 200L;
        Freelancer mockFreelancer = Freelancer.create(userId, "프론트엔드 개발자", FreelancerGrade.JUNIOR);
        given(freelancerRepository.findByUserId(userId)).willReturn(Optional.of(mockFreelancer));

        FreelancerProfileUpdateRequestDto request = new FreelancerProfileUpdateRequestDto(
                "풀스택 개발자",
                "안녕하세요. 풀스택 개발자입니다.",
                5,
                50000L,
                List.of("Java", "React", "Spring"),
                "팀",
                LocalDate.of(2026, 3, 15),
                "혼합",
                "부산",
                "지드래곤",
                4,
                5,
                3,
                4,
                4,
                2,
                4.5
        );

        // when
        freelancerProfileService.updateProfile(userId, request);

        // then
        verify(freelancerRepository, times(1)).findByUserId(userId);
        verify(sharedMypageApi, times(1)).updateUserName(userId, "지드래곤");
        assertThat(mockFreelancer.getJob()).isEqualTo("풀스택 개발자");
        assertThat(mockFreelancer.getIntroduction()).isEqualTo("안녕하세요. 풀스택 개발자입니다.");
        assertThat(mockFreelancer.getCareerYears()).isEqualTo(5);
        assertThat(mockFreelancer.getWage()).isEqualTo(50000L);
        assertThat(mockFreelancer.getSkills()).containsExactlyInAnyOrder("Java", "React", "Spring");
        assertThat(mockFreelancer.getWorkConditions()).isNotNull();
        assertThat(mockFreelancer.getWorkConditions().getConditionsType()).isEqualTo("팀");
        assertThat(mockFreelancer.getWorkConditions().getStartDate()).isEqualTo(LocalDate.of(2026, 3, 15));
        assertThat(mockFreelancer.getWorkConditions().getWorkStyle()).isEqualTo("혼합");
        assertThat(mockFreelancer.getWorkConditions().getLocation()).isEqualTo("부산");
    }

    @Test
    @DisplayName("[TDD] 4. 프로필 수정: 존재하지 않는 userId면 USER_NOT_FOUND 예외 발생")
    void updateProfile_NotFound_ThrowsException() {
        // given
        Long userId = 999L;
        given(freelancerRepository.findByUserId(userId)).willReturn(Optional.empty());

        FreelancerProfileUpdateRequestDto request = new FreelancerProfileUpdateRequestDto(
                "풀스택 개발자",
                "소개",
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

        // when & then
        assertThatThrownBy(() -> freelancerProfileService.updateProfile(userId, request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.USER_NOT_FOUND);
    }

    @Test
    @DisplayName("[TDD] 5. 프로필 이미지 업로드: 정상 요청 시 S3 업로드 및 URL 반영")
    void updateAvatarUrl_Success() throws Exception {
        // given
        Long userId = 300L;
        Freelancer mockFreelancer = Freelancer.create(userId, "디자이너", FreelancerGrade.SENIOR);
        given(freelancerRepository.findByUserId(userId)).willReturn(Optional.of(mockFreelancer));

        // 유효한 PNG 매직 바이트
        byte[] validPngBytes = new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
        MockMultipartFile mockFile = new MockMultipartFile("file", "avatar.png", "image/png", validPngBytes);

        String uploadedUrl = "avatars/mock-uuid.png";
        given(fileStorage.upload(any(byte[].class), anyString(), eq("image/png"))).willReturn(uploadedUrl);

        // when
        String result = freelancerProfileService.updateAvatarUrl(userId, mockFile);

        // then
        verify(freelancerRepository, times(1)).findByUserId(userId);
        verify(fileStorage, times(1)).upload(any(byte[].class), anyString(), eq("image/png"));
        assertThat(result).isEqualTo(uploadedUrl);
    }

    @Test
    @DisplayName("[TDD] 6. 프로필 이미지 업로드: 5MB 초과 파일이면 INVALID_INPUT_VALUE")
    void updateAvatarUrl_TooLargeFile_ThrowsException() {
        // given
        Long userId = 300L;
        byte[] largeFileBytes = new byte[6 * 1024 * 1024]; // 6MB
        MockMultipartFile oversizedFile = new MockMultipartFile("file", "big.png", "image/png", largeFileBytes);

        // when & then
        assertThatThrownBy(() -> freelancerProfileService.updateAvatarUrl(userId, oversizedFile))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_INPUT_VALUE);
    }

    @Test
    @DisplayName("[TDD] 7. 프로필 이미지 업로드: 잘못된 MIME 타입이면 INVALID_INPUT_VALUE")
    void updateAvatarUrl_InvalidContentType_ThrowsException() {
        // given
        Long userId = 300L;
        MockMultipartFile invalidFile = new MockMultipartFile("file", "script.txt", "text/plain", "hello".getBytes());

        // when & then
        assertThatThrownBy(() -> freelancerProfileService.updateAvatarUrl(userId, invalidFile))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_INPUT_VALUE);
    }
}
