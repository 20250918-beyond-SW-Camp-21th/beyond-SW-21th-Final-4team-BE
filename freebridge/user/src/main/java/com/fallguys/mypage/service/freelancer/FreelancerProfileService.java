package com.fallguys.mypage.service.freelancer;

import com.fallguys.common.ai.port.RecommendationEngine;
import com.fallguys.common.exception.BusinessException;
import com.fallguys.common.exception.ErrorCode;
import com.fallguys.common.port.FileStorage;
import com.fallguys.mypage.api.shared.SharedMypageApi;
import com.fallguys.mypage.api.web.dto.freelancer.request.FreelancerProfileUpdateRequestDto;
import com.fallguys.mypage.api.web.dto.freelancer.response.CollaborationDto;
import com.fallguys.mypage.api.web.dto.freelancer.response.CrmAlertsDto;
import com.fallguys.mypage.api.web.dto.freelancer.response.ExpertiseDto;
import com.fallguys.mypage.api.web.dto.freelancer.response.FreelancerBasicProfileDto;
import com.fallguys.mypage.api.web.dto.freelancer.response.FreelancerProfileResponseDto;
import com.fallguys.mypage.api.web.dto.freelancer.response.FreelancerProjectStatusStatsDto;
import com.fallguys.mypage.api.web.dto.freelancer.response.FreelancerStatsDto;
import com.fallguys.mypage.api.web.dto.freelancer.response.PortfolioInfoDto;
import com.fallguys.mypage.api.web.dto.freelancer.response.WorkConditionsDto;
import com.fallguys.mypage.entity.freelancer.Collaboration;
import com.fallguys.mypage.entity.freelancer.Expertise;
import com.fallguys.mypage.entity.freelancer.Freelancer;
import com.fallguys.mypage.entity.freelancer.FreelancerStatus;
import com.fallguys.mypage.entity.freelancer.PortfolioInfo;
import com.fallguys.mypage.entity.freelancer.WorkConditions;
import com.fallguys.mypage.repository.freelancer.FreelancerRepository;
import com.fallguys.user.api.shared.response.ExternalUserResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class FreelancerProfileService {

    private static final String JOB_RECOMMENDATION_CACHE_KEY_PREFIX = "ai:reco:jobs:v3:";
    private final FreelancerRepository freelancerRepository;
    private final FileStorage fileStorage;
    private final SharedMypageApi sharedMypageApi;
    private final FreelancerReviewService freelancerReviewService;
    private final RecommendationEngine recommendationEngine;
    private final FreelancerProjectService freelancerProjectService;
    private final RedisTemplate<String, Object> redisTemplate;

    private static final int MIN_APPLICATIONS_FOR_PORTFOLIO_IMPROVEMENT = 5;
    private static final double PORTFOLIO_SUCCESS_RATIO_THRESHOLD = 0.2;
    private static final int MIN_COMPLETED_PROJECTS_FOR_RATE_BUMP = 2;
    private static final double MIN_AVERAGE_RATE_FOR_BUMP = 4.0;
    private static final int IN_PROGRESS_BURNOUT_THRESHOLD = 3;
    private static final int MIN_COMPLETED_PROJECTS_FOR_CHURN_WARNING = 1;

    private static final long MAX_AVATAR_BYTES = 5 * 1024 * 1024; // 프로필 이미지 최대 허용 크기 5MB

    @Transactional
    public FreelancerProfileResponseDto getProfile(Long userId) {
        freelancerReviewService.syncReviewMetricsToFreelancer(userId);
        Freelancer freelancer = findByUserIdOrThrow(userId);
        ExternalUserResponse userResponse = sharedMypageApi.getUserById(userId);
        freelancerReviewService.getReviewSummary(userId);

        WorkConditions workConditions = freelancer.getWorkConditions();
        WorkConditionsDto workConditionsDto = workConditions == null ? null : new WorkConditionsDto(
                workConditions.getConditionsType(),
                workConditions.getStartDate(),
                workConditions.getWorkStyle(),
                workConditions.getLocation()
        );

        Expertise expertise = freelancer.getExpertise();
        ExpertiseDto expertiseDto = expertise == null ? null : new ExpertiseDto(
                expertise.getProgramming(),
                expertise.getFramework(),
                expertise.getProblemSolving()
        );

        Collaboration collaboration = freelancer.getCollaboration();
        CollaborationDto collaborationDto = collaboration == null ? null : new CollaborationDto(
                collaboration.getCommunication(),
                collaboration.getScheduleAdherence(),
                collaboration.getDispute()
        );

        PortfolioInfo portfolioInfo = freelancer.getPortfolioInfo();
        PortfolioInfoDto portfolioInfoDto = portfolioInfo == null ? null : new PortfolioInfoDto(
                toAccessibleUrl(portfolioInfo.getPortfolioFileUrl()),
                portfolioInfo.getPortfolioFileName(),
                portfolioInfo.getPortfolioLastUpdated()
        );

        FreelancerProjectStatusStatsDto projectStats = freelancerProjectService.getProjectStats(freelancer.getFreelancerId());
        CrmAlertsDto crmAlerts = buildCrmAlerts(freelancer, projectStats);

        FreelancerBasicProfileDto basicProfile = new FreelancerBasicProfileDto(
                toAccessibleUrl(freelancer.getAvatarUrl()),
                userResponse != null ? userResponse.getName() : null,
                userResponse != null ? userResponse.getEmail() : null,
                null,
                freelancer.getJob(),
                freelancer.getIntroduction(),
                freelancer.getGrade() != null ? freelancer.getGrade().name() : null,
                freelancer.getCareerYears(),
                freelancer.getWage(),
                freelancer.getSkills(),
                freelancer.getStatus() != null ? freelancer.getStatus().name() : null,
                workConditionsDto,
                expertiseDto,
                collaborationDto,
                calculateTotalScore(expertise, collaboration, freelancer.getAverageRate()),
                portfolioInfoDto,
                crmAlerts
        );

        FreelancerStatsDto stats = new FreelancerStatsDto(
                freelancer.getStatContact(),
                freelancer.getStatChat(),
                freelancer.getStatContract()
        );

        return new FreelancerProfileResponseDto(basicProfile, stats);
    }

    @Transactional
    public void updateProfile(Long userId, FreelancerProfileUpdateRequestDto request) {
        Freelancer freelancer = findByUserIdOrThrow(userId);

        freelancer.updateBasicProfile(request.job(), null, request.introduction());
        freelancer.updateCareer(request.careerYears(), request.wage());
        freelancer.replaceSkills(request.skills());

        if (request.name() != null && !request.name().isBlank()) {
            sharedMypageApi.updateUserName(userId, request.name());
        }

        boolean hasWorkConditions = request.workType() != null
                || request.availableStartDate() != null
                || request.workStyle() != null
                || request.workLocation() != null;

        if (hasWorkConditions) {
            WorkConditions existing = freelancer.getWorkConditions();
            String workType = request.workType() != null
                    ? request.workType()
                    : existing != null ? existing.getConditionsType() : null;
            var availableStartDate = request.availableStartDate() != null
                    ? request.availableStartDate()
                    : existing != null ? existing.getStartDate() : null;
            String workStyle = request.workStyle() != null
                    ? request.workStyle()
                    : existing != null ? existing.getWorkStyle() : null;
            String workLocation = request.workLocation() != null
                    ? request.workLocation()
                    : existing != null ? existing.getLocation() : null;
            WorkConditions workConditions = new WorkConditions(
                    workType,
                    availableStartDate,
                    workStyle,
                    workLocation
            );
            freelancer.updateWorkConditions(workConditions);
        }

        boolean hasExpertise = request.expertiseProgramming() != null
                || request.expertiseFramework() != null
                || request.expertiseProblemSolving() != null;

        if (hasExpertise) {
            Expertise existing = freelancer.getExpertise();
            Double programming = request.expertiseProgramming() != null
                    ? request.expertiseProgramming().doubleValue()
                    : existing != null ? existing.getProgramming() : null;
            Double framework = request.expertiseFramework() != null
                    ? request.expertiseFramework().doubleValue()
                    : existing != null ? existing.getFramework() : null;
            Double problemSolving = request.expertiseProblemSolving() != null
                    ? request.expertiseProblemSolving().doubleValue()
                    : existing != null ? existing.getProblemSolving() : null;
            Expertise expertise = new Expertise(
                    programming,
                    framework,
                    problemSolving
            );
            freelancer.updateExpertise(expertise);
        }

        boolean hasCollaboration = request.collaborationCommunication() != null
                || request.collaborationScheduleAdherence() != null
                || request.collaborationDispute() != null;

        if (hasCollaboration) {
            Collaboration existing = freelancer.getCollaboration();
            Double communication = request.collaborationCommunication() != null
                    ? request.collaborationCommunication().doubleValue()
                    : existing != null ? existing.getCommunication() : null;
            Double scheduleAdherence = request.collaborationScheduleAdherence() != null
                    ? request.collaborationScheduleAdherence().doubleValue()
                    : existing != null ? existing.getScheduleAdherence() : null;
            Double dispute = request.collaborationDispute() != null
                    ? request.collaborationDispute().doubleValue()
                    : existing != null ? existing.getDispute() : null;
            Collaboration collaboration = new Collaboration(
                    communication,
                    scheduleAdherence,
                    dispute
            );
            freelancer.updateCollaboration(collaboration);
        }

        if (request.averageRating() != null) {
            freelancer.updateAverageRate(request.averageRating());
        }

        runAfterCommitSafely(() -> {
            try {
                evictFreelancerJobRecommendationCache(userId);
            } catch (RuntimeException e) {
                log.warn("Failed to evict freelancer job recommendation cache. userId={}", userId, e);
            }
            syncFreelancerProfileToAi(freelancer);
        });
    }

    @Transactional
    public String updateAvatarUrl(Long userId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }

        if (file.getSize() > MAX_AVATAR_BYTES) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }

        String contentType = file.getContentType();
        Set<String> allowedTypes = Set.of("image/jpeg", "image/png", "image/webp", "image/gif");
        if (contentType == null || !allowedTypes.contains(contentType.toLowerCase())) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }

        String uploadKey = null;
        try {
            byte[] fileBytes = file.getBytes();
            if (!isValidImageByMagicBytes(fileBytes)) {
                throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
            }

            Freelancer freelancer = findByUserIdOrThrow(userId);

            String extension = getExtension(file.getOriginalFilename());
            uploadKey = "freelancers/avatar/" + UUID.randomUUID() + extension;
            String uploadedKey = fileStorage.upload(fileBytes, uploadKey, file.getContentType());
            String previousKey = freelancer.getAvatarUrl();

            // DB 롤백 시 이미 업로드된 S3 파일 삭제 (고아 파일 방지)
            String finalUploadKey = uploadedKey;
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCompletion(int status) {
                    if (status == STATUS_ROLLED_BACK) {
                        try {
                            fileStorage.deleteByKey(finalUploadKey);
                        } catch (Exception ex) {
                            log.error("S3 롤백 파일 삭제 실패 - key: {}", finalUploadKey, ex);
                        }
                    }
                }
            });

            if (isStoredKey(previousKey) && !previousKey.equals(uploadedKey)) {
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        try {
                            fileStorage.deleteByKey(previousKey);
                        } catch (Exception ex) {
                            log.error("S3 old avatar delete failed. key: {}", previousKey, ex);
                        }
                    }
                });
            }

            freelancer.updateBasicProfile(null, uploadedKey, null);

            return toAccessibleUrl(uploadedKey);
        } catch (BusinessException e) {
            throw e;
        } catch (IOException e) {
            log.error("S3 업로드 실패 - userId: {}, fileName: {}", userId, file.getOriginalFilename(), e);
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR);
        } catch (RuntimeException e) {
            log.error("S3 업로드 실패 - userId: {}, key: {}", userId, uploadKey, e);
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    private Freelancer findByUserIdOrThrow(Long userId) {
        return freelancerRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }

    private void runAfterCommitSafely(Runnable task) {
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    try {
                        task.run();
                    } catch (RuntimeException e) {
                        log.warn("AI sync failed in runAfterCommitSafely", e);
                    }
                }
            });
            return;
        }

        try {
            task.run();
        } catch (RuntimeException e) {
            log.warn("AI sync failed in runAfterCommitSafely", e);
        }
    }

    private void syncFreelancerProfileToAi(Freelancer freelancer) {
        recommendationEngine.syncToAiServer(
                freelancer.getFreelancerId(),
                freelancer.getFreelancerId(),
                "new_profile",
                buildFreelancerProfileAiContent(freelancer),
                Optional.ofNullable(freelancer.getStatus()).map(Enum::name).orElse("POTENTIAL")
        );
    }

    private void evictFreelancerJobRecommendationCache(Long userId) {
        redisTemplate.delete(JOB_RECOMMENDATION_CACHE_KEY_PREFIX + userId);
    }

    private String buildFreelancerProfileAiContent(Freelancer freelancer) {
        StringBuilder builder = new StringBuilder();
        builder.append("Job: ").append(Optional.ofNullable(freelancer.getJob()).orElse("")).append('\n');
        builder.append("Introduction: ").append(Optional.ofNullable(freelancer.getIntroduction()).orElse("")).append('\n');
        builder.append("Skills: ").append(String.join(", ", Optional.ofNullable(freelancer.getSkills()).orElseGet(java.util.List::of))).append('\n');
        builder.append("Career Years: ").append(Optional.ofNullable(freelancer.getCareerYears()).orElse(0)).append('\n');
        builder.append("Wage: ").append(Optional.ofNullable(freelancer.getWage()).orElse(0L)).append('\n');
        builder.append("Grade: ").append(Optional.ofNullable(freelancer.getGrade()).map(Enum::name).orElse("")).append('\n');
        builder.append("Status: ").append(Optional.ofNullable(freelancer.getStatus()).map(Enum::name).orElse("POTENTIAL")).append('\n');
        builder.append("Expertise Average Rate: ").append(
                Optional.ofNullable(calculateExpertiseAverage(freelancer.getExpertise()))
                        .map(Object::toString)
                        .orElse("")
        ).append('\n');
        builder.append("Collaboration Average Rate: ").append(
                Optional.ofNullable(calculateCollaborationAverage(freelancer.getCollaboration()))
                        .map(Object::toString)
                        .orElse("")
        ).append('\n');
        builder.append("Average Rate: ").append(
                Optional.ofNullable(freelancer.getAverageRate())
                        .map(Object::toString)
                        .orElse("")
        ).append('\n');

        WorkConditions workConditions = freelancer.getWorkConditions();
        if (workConditions != null) {
            builder.append("Work Type: ").append(Optional.ofNullable(workConditions.getConditionsType()).orElse("")).append('\n');
            builder.append("Available Start Date: ").append(Optional.ofNullable(workConditions.getStartDate()).map(Object::toString).orElse("")).append('\n');
            builder.append("Work Style: ").append(Optional.ofNullable(workConditions.getWorkStyle()).orElse("")).append('\n');
            builder.append("Work Location: ").append(Optional.ofNullable(workConditions.getLocation()).orElse("")).append('\n');
        }

        return builder.toString();
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
        if ((bytes[0] & 0xFF) == 0x89 && (bytes[1] & 0xFF) == 0x50 && (bytes[2] & 0xFF) == 0x4E && (bytes[3] & 0xFF) == 0x47
                && (bytes[4] & 0xFF) == 0x0D && (bytes[5] & 0xFF) == 0x0A && (bytes[6] & 0xFF) == 0x1A && (bytes[7] & 0xFF) == 0x0A) {
            return true;
        }
        // GIF87a / GIF89a
        if ((bytes[0] & 0xFF) == 0x47 && (bytes[1] & 0xFF) == 0x49 && (bytes[2] & 0xFF) == 0x46 && (bytes[3] & 0xFF) == 0x38
                && ((bytes[4] & 0xFF) == 0x37 || (bytes[4] & 0xFF) == 0x39) && (bytes[5] & 0xFF) == 0x61) {
            return true;
        }
        // WEBP: RIFF....WEBP
        if (bytes.length >= 12
                && (bytes[0] & 0xFF) == 0x52 && (bytes[1] & 0xFF) == 0x49 && (bytes[2] & 0xFF) == 0x46 && (bytes[3] & 0xFF) == 0x46
                && (bytes[8] & 0xFF) == 0x57 && (bytes[9] & 0xFF) == 0x45 && (bytes[10] & 0xFF) == 0x42 && (bytes[11] & 0xFF) == 0x50) {
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

    private Double calculateTotalScore(Expertise expertise, Collaboration collaboration, Double fallbackAverageRate) {
        Double expertiseAverage = calculateExpertiseAverage(expertise);
        Double collaborationAverage = calculateCollaborationAverage(collaboration);

        if (expertiseAverage == null) {
            expertiseAverage = fallbackAverageRate;
        }
        if (collaborationAverage == null) {
            collaborationAverage = fallbackAverageRate;
        }

        if (expertiseAverage == null && collaborationAverage == null) {
            return null;
        }
        if (expertiseAverage == null) {
            return collaborationAverage;
        }
        if (collaborationAverage == null) {
            return expertiseAverage;
        }

        return (expertiseAverage + collaborationAverage) / 2.0;
    }

    private Double calculateExpertiseAverage(Expertise expertise) {
        if (expertise == null) {
            return null;
        }

        int count = 0;
        double total = 0.0;

        if (expertise.getProgramming() != null) {
            total += expertise.getProgramming();
            count++;
        }
        if (expertise.getFramework() != null) {
            total += expertise.getFramework();
            count++;
        }
        if (expertise.getProblemSolving() != null) {
            total += expertise.getProblemSolving();
            count++;
        }

        return count == 0 ? null : total / count;
    }

    private Double calculateCollaborationAverage(Collaboration collaboration) {
        if (collaboration == null) {
            return null;
        }

        int count = 0;
        double total = 0.0;

        if (collaboration.getCommunication() != null) {
            total += collaboration.getCommunication();
            count++;
        }
        if (collaboration.getScheduleAdherence() != null) {
            total += collaboration.getScheduleAdherence();
            count++;
        }
        if (collaboration.getDispute() != null) {
            total += collaboration.getDispute();
            count++;
        }

        return count == 0 ? null : total / count;
    }

    private CrmAlertsDto buildCrmAlerts(Freelancer freelancer, FreelancerProjectStatusStatsDto projectStats) {
        int appliedProjects = safeInt(projectStats.appliedProjects());
        int inProgressProjects = safeInt(projectStats.inProgressProjects());
        int completedProjects = safeInt(projectStats.completedProjects());

        boolean isOnboardingNeeded = freelancer.getStatus() == FreelancerStatus.POTENTIAL
                && hasMultipleIncompleteProfileSections(freelancer);
        boolean isApplyEncouraged = !isOnboardingNeeded
                && (freelancer.getStatus() == FreelancerStatus.POTENTIAL
                || freelancer.getStatus() == FreelancerStatus.CONTRACT_EXPIRED)
                && inProgressProjects == 0
                && appliedProjects == 0
                && isProfileBasicallyComplete(freelancer);
        boolean isPortfolioImproveNeeded = appliedProjects >= MIN_APPLICATIONS_FOR_PORTFOLIO_IMPROVEMENT
                && ((double) (inProgressProjects + completedProjects) / appliedProjects) < PORTFOLIO_SUCCESS_RATIO_THRESHOLD;
        boolean isRateBumpEligible = completedProjects >= MIN_COMPLETED_PROJECTS_FOR_RATE_BUMP
                && safeDouble(freelancer.getAverageRate()) >= MIN_AVERAGE_RATE_FOR_BUMP;
        boolean isBurnoutWarning = inProgressProjects >= IN_PROGRESS_BURNOUT_THRESHOLD;
        boolean isChurnWarning = completedProjects >= MIN_COMPLETED_PROJECTS_FOR_CHURN_WARNING
                && freelancer.getStatus() != FreelancerStatus.LEFT
                && inProgressProjects == 0
                && appliedProjects == 0;

        return new CrmAlertsDto(
                isOnboardingNeeded,
                isApplyEncouraged,
                isPortfolioImproveNeeded,
                isRateBumpEligible,
                isBurnoutWarning,
                isChurnWarning
        );
    }

    private boolean hasMultipleIncompleteProfileSections(Freelancer freelancer) {
        int incompleteCount = 0;
        if (!hasText(freelancer.getJob())) {
            incompleteCount++;
        }
        if (!hasEnoughSkills(freelancer)) {
            incompleteCount++;
        }
        if (!hasText(freelancer.getIntroduction())) {
            incompleteCount++;
        }
        if (freelancer.getPortfolioInfo() == null) {
            incompleteCount++;
        }
        return incompleteCount >= 2;
    }

    private boolean isProfileBasicallyComplete(Freelancer freelancer) {
        return hasText(freelancer.getJob())
                && hasEnoughSkills(freelancer)
                && hasText(freelancer.getIntroduction());
    }

    private boolean hasEnoughSkills(Freelancer freelancer) {
        return freelancer.getSkills() != null && freelancer.getSkills().size() >= 2;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private int safeInt(Integer value) {
        return value == null ? 0 : value;
    }

    private double safeDouble(Double value) {
        return value == null ? 0.0 : value;
    }
}



