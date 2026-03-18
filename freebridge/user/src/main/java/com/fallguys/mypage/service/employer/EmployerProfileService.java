package com.fallguys.mypage.service.employer;

import com.fallguys.common.port.FileStorage;
import com.fallguys.mypage.api.web.dto.employer.request.EmployerProfileUpdateRequestDto;
import com.fallguys.mypage.api.web.dto.employer.response.CrmAlertsResponseDto;
import com.fallguys.mypage.api.web.dto.employer.response.EmployerBasicProfileDto;
import com.fallguys.mypage.api.web.dto.employer.response.EmployerProjectStatsResponseDto;
import com.fallguys.mypage.entity.employer.Employer;
import com.fallguys.mypage.entity.employer.EmployerStatus;
import com.fallguys.mypage.entity.employer.Scale;
import com.fallguys.mypage.entity.employer.Subscription;
import com.fallguys.mypage.repository.employer.EmployerRepository;
import com.fallguys.user.api.shared.ExternalUserApi;
import com.fallguys.user.api.shared.response.ExternalUserMyInfoResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmployerProfileService {

    private final EmployerRepository employerRepository;
    private final FileStorage fileStorage;
    private final ExternalUserApi externalUserApi;
    private final EmployerReviewService employerReviewService;
    private final EmployerProjectService employerProjectService;

    @Transactional(readOnly = true)
    public EmployerBasicProfileDto getProfile(Long userId) {
        Employer employer = employerRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("해당 유저의 고용주 프로필을 찾을 수 없습니다."));
        employerReviewService.getReputationSummary(userId);

        ExternalUserMyInfoResponse userInfo = null;
        try {
            userInfo = externalUserApi.getMyInfo(userId);
        } catch (com.fallguys.common.exception.BusinessException e) {
            if (e.getErrorCode() != com.fallguys.common.exception.ErrorCode.USER_NOT_FOUND) {
                throw e;
            }
        }
        String phone = userInfo != null ? userInfo.getPhone() : null;
        return new EmployerBasicProfileDto(
                employer.getCompanyName(),
                employer.getIndustry(),
                employer.getScale() != null ? employer.getScale().name() : null,
                employer.getLocation(),
                employer.getWebsiteUrl(),
                phone,
                employer.getDescription(),
                toAccessibleUrl(employer.getLogoUrl()),
                employer.getStatus() != null ? employer.getStatus().name() : null
        );
    }

    @Transactional
    public void updateProfile(Long userId, EmployerProfileUpdateRequestDto request) {
        Employer employer = employerRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("해당 유저의 고용주 프로필을 찾을 수 없습니다."));

        String companyName = hasText(request.companyName()) ? request.companyName() : employer.getCompanyName();
        String industry = request.industry() != null ? request.industry() : employer.getIndustry();
        Scale scale = (request.scale() != null && !request.scale().isBlank())
                ? parseScale(request.scale())
                : employer.getScale();
        String location = hasText(request.location()) ? request.location() : employer.getLocation();
        String websiteUrl = hasText(request.websiteUrl()) ? request.websiteUrl() : employer.getWebsiteUrl();
        String description = hasText(request.description()) ? request.description() : employer.getDescription();

        employer.updateProfile(
                companyName,
                industry,
                scale,
                location,
                websiteUrl,
                description,
                employer.getLogoUrl() // 기존 로고는 유지 (로고 수정 API 분리)
        );

        if (hasText(request.phone())) {
            try {
                externalUserApi.updatePhone(userId, request.phone());
            } catch (com.fallguys.common.exception.BusinessException e) {
                if (e.getErrorCode() == com.fallguys.common.exception.ErrorCode.USER_NOT_FOUND) {
                    log.error("Inconsistent employer/user state while updating phone. employerId={}, userId={}", employer.getEmployerId(), userId);
                    throw new com.fallguys.common.exception.BusinessException(
                            com.fallguys.common.exception.ErrorCode.INTERNAL_SERVER_ERROR
                    );
                }
                throw e;
            }
        }
    }

    private Scale parseScale(String scaleStr) {
        if (scaleStr == null || scaleStr.isBlank()) {
            return null;
        }
        try {
            return Scale.valueOf(scaleStr);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("유효하지 않은 기업 규모(Scale) 값입니다: " + scaleStr);
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    @Transactional
    public String updateLogoUrl(Long userId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("업로드할 파일이 없습니다.");
        }

        long MAX_LOGO_BYTES = 5 * 1024 * 1024; // 5MB
        if (file.getSize() > MAX_LOGO_BYTES) {
            throw new IllegalArgumentException("업로드 파일 크기는 5MB를 초과할 수 없습니다.");
        }

        String contentType = file.getContentType();
        Set<String> allowedMimeTypes = Set.of("image/jpeg", "image/png", "image/webp", "image/gif");

        if (contentType == null || !allowedMimeTypes.contains(contentType.toLowerCase())) {
            throw new IllegalArgumentException("이미지 파일(JPEG, PNG, WEBP, GIF)만 업로드 가능합니다. (현재 타입: " + contentType + ")");
        }

        try {
            byte[] fileBytes = file.getBytes();
            if (!isValidImageByMagicBytes(fileBytes)) {
                throw new IllegalArgumentException("올바른 이미지 파일 형식이 아닙니다. (확장자 위조 의심)");
            }

            Employer employer = employerRepository.findByUserId(userId)
                    .orElseThrow(() -> new IllegalArgumentException("해당 유저의 고용주 프로필을 찾을 수 없습니다."));

            String extension = getExtension(file.getOriginalFilename());
            String key = "employers/logo/" + UUID.randomUUID() + extension;
            String uploadedKey = fileStorage.upload(fileBytes, key, file.getContentType());
            String previousKey = employer.getLogoUrl();

            String finalUploadKey = uploadedKey;
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCompletion(int status) {
                    if (status == STATUS_ROLLED_BACK) {
                        try {
                            fileStorage.deleteByKey(finalUploadKey);
                        } catch (Exception ex) {
                            log.error("S3 rollback delete failed. key: {}", finalUploadKey, ex);
                        }
                    }
                }
            });

            employer.updateLogoUrl(uploadedKey);

            if (isStoredKey(previousKey) && !previousKey.equals(uploadedKey)) {
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        try {
                            fileStorage.deleteByKey(previousKey);
                        } catch (Exception ex) {
                            log.error("S3 old logo delete failed. key: {}", previousKey, ex);
                        }
                    }
                });
            }

            return toAccessibleUrl(uploadedKey);
        } catch (IOException e) {
            throw new RuntimeException("파일 업로드 중 오류가 발생했습니다.", e);
        }
    }

    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf("."));
    }

    private boolean isValidImageByMagicBytes(byte[] bytes) {
        if (bytes == null || bytes.length < 8) {
            return false;
        }

        // JPEG: FF D8 FF
        if ((bytes[0] & 0xFF) == 0xFF && (bytes[1] & 0xFF) == 0xD8 && (bytes[2] & 0xFF) == 0xFF) {
            return true;
        }
        // PNG: 89 50 4E 47 0D 0A 1A 0A
        if ((bytes[0] & 0xFF) == 0x89 && (bytes[1] & 0xFF) == 0x50 && (bytes[2] & 0xFF) == 0x4E && (bytes[3] & 0xFF) == 0x47 &&
            (bytes[4] & 0xFF) == 0x0D && (bytes[5] & 0xFF) == 0x0A && (bytes[6] & 0xFF) == 0x1A && (bytes[7] & 0xFF) == 0x0A) {
            return true;
        }
        // GIF87a / GIF89a: 47 49 46 38 37 61 / 47 49 46 38 39 61
        if ((bytes[0] & 0xFF) == 0x47 && (bytes[1] & 0xFF) == 0x49 && (bytes[2] & 0xFF) == 0x46 && (bytes[3] & 0xFF) == 0x38 &&
            ((bytes[4] & 0xFF) == 0x37 || (bytes[4] & 0xFF) == 0x39) && (bytes[5] & 0xFF) == 0x61) {
            return true;
        }
        // WEBP: RIFF .... WEBP
        if ((bytes[0] & 0xFF) == 0x52 && (bytes[1] & 0xFF) == 0x49 && (bytes[2] & 0xFF) == 0x46 && (bytes[3] & 0xFF) == 0x46 &&
            bytes.length >= 12 &&
            (bytes[8] & 0xFF) == 0x57 && (bytes[9] & 0xFF) == 0x45 && (bytes[10] & 0xFF) == 0x42 && (bytes[11] & 0xFF) == 0x50) {
            return true;
        }

        return false;
    }

    private String toAccessibleUrl(String storedKeyOrUrl) {
        if (storedKeyOrUrl == null || storedKeyOrUrl.isBlank()) {
            return null;
        }
        if (storedKeyOrUrl.startsWith("http://") || storedKeyOrUrl.startsWith("https://")) {
            return storedKeyOrUrl;
        }
        return fileStorage.generatePresignedUrl(storedKeyOrUrl);
    }

    private boolean isStoredKey(String storedKeyOrUrl) {
        return storedKeyOrUrl != null
                && !storedKeyOrUrl.isBlank()
                && !storedKeyOrUrl.startsWith("http://")
                && !storedKeyOrUrl.startsWith("https://");
    }

    @Transactional(readOnly = true)
    public CrmAlertsResponseDto getCrmAlerts(Long userId) {
        Employer employer = employerRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("해당 유저의 고용주 프로필을 찾을 수 없습니다."));

        // BASIC 구독 플랜인 경우 업셀 대상으로 간주
        // Recruitment module caches employer project stats by employer userId.
        EmployerProjectStatsResponseDto projectStats = employerProjectService.getProjectStats(userId);

        int totalProjects = safeInt(projectStats.totalProjects());
        int activeApplicants = safeInt(projectStats.activeApplicants());
        int contractedFreelancers = safeInt(projectStats.contractedFreelancers());

        boolean isFirstJobEncouraged =
                (employer.getStatus() == EmployerStatus.POTENTIAL || employer.getStatus() == EmployerStatus.ACTIVE)
                        && totalProjects == 0;
        boolean hasPendingApplicants = activeApplicants > 0;
        boolean isContractConversionNeeded = activeApplicants >= 3 && contractedFreelancers == 0;
        boolean isRehiringRecommended = contractedFreelancers >= 1 && activeApplicants == 0 && totalProjects >= 1;
        boolean isSubscriptionAttentionNeeded = employer.getPendingSubscription() != null
                || (employer.getSubscription() != Subscription.BASIC && employer.getNextBillingDate() != null);
        boolean isPremiumUpsellEligible = employer.getSubscription() == Subscription.BASIC
                && (activeApplicants >= 5 || totalProjects >= 2);
        boolean isPrimeUpsellEligible = employer.getSubscription() == Subscription.PRO
                && (contractedFreelancers >= 2 || totalProjects >= 3);
        String upsellTarget = isPrimeUpsellEligible ? "PRIME" : isPremiumUpsellEligible ? "PRO" : null;

        return new CrmAlertsResponseDto(
                isFirstJobEncouraged,
                hasPendingApplicants,
                isContractConversionNeeded,
                isRehiringRecommended,
                isSubscriptionAttentionNeeded,
                isPremiumUpsellEligible,
                isPrimeUpsellEligible,
                upsellTarget
        );
    }

    private int safeInt(Integer value) {
        return value == null ? 0 : value;
    }
}
