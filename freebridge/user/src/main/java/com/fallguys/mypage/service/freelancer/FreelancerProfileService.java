package com.fallguys.mypage.service.freelancer;

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
import com.fallguys.mypage.api.web.dto.freelancer.response.FreelancerStatsDto;
import com.fallguys.mypage.api.web.dto.freelancer.response.PortfolioInfoDto;
import com.fallguys.mypage.api.web.dto.freelancer.response.WorkConditionsDto;
import com.fallguys.mypage.entity.freelancer.Collaboration;
import com.fallguys.mypage.entity.freelancer.Expertise;
import com.fallguys.mypage.entity.freelancer.Freelancer;
import com.fallguys.mypage.entity.freelancer.PortfolioInfo;
import com.fallguys.mypage.entity.freelancer.WorkConditions;
import com.fallguys.mypage.repository.freelancer.FreelancerRepository;
import com.fallguys.user.api.shared.response.ExternalUserResponse;
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
public class FreelancerProfileService {

    private final FreelancerRepository freelancerRepository;
    private final FileStorage fileStorage;
    private final SharedMypageApi sharedMypageApi;

    private static final long MAX_AVATAR_BYTES = 5 * 1024 * 1024; // ?꾨줈???대?吏 理쒕? 5MB

    @Transactional(readOnly = true)
    public FreelancerProfileResponseDto getProfile(Long userId) {
        Freelancer freelancer = findByUserIdOrThrow(userId);
        ExternalUserResponse userResponse = sharedMypageApi.getUserById(userId);

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
                portfolioInfo.getPortfolioFileUrl(),
                portfolioInfo.getPortfolioFileName(),
                portfolioInfo.getPortfolioLastUpdated()
        );

        CrmAlertsDto crmAlerts = new CrmAlertsDto(null, null, null);

        FreelancerBasicProfileDto basicProfile = new FreelancerBasicProfileDto(
                freelancer.getAvatarUrl(),
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
            Integer programming = request.expertiseProgramming() != null
                    ? request.expertiseProgramming()
                    : existing != null ? existing.getProgramming() : null;
            Integer framework = request.expertiseFramework() != null
                    ? request.expertiseFramework()
                    : existing != null ? existing.getFramework() : null;
            Integer problemSolving = request.expertiseProblemSolving() != null
                    ? request.expertiseProblemSolving()
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
            Integer communication = request.collaborationCommunication() != null
                    ? request.collaborationCommunication()
                    : existing != null ? existing.getCommunication() : null;
            Integer scheduleAdherence = request.collaborationScheduleAdherence() != null
                    ? request.collaborationScheduleAdherence()
                    : existing != null ? existing.getScheduleAdherence() : null;
            Integer dispute = request.collaborationDispute() != null
                    ? request.collaborationDispute()
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
            String uploadedUrl = fileStorage.upload(fileBytes, uploadKey, file.getContentType());

            // DB 濡ㅻ갚 ???대? ?낅줈?쒕맂 S3 ?뚯씪 ??젣 (怨좎븘 ?뚯씪 諛⑹?)
            String finalUploadKey = uploadKey;
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCompletion(int status) {
                    if (status == STATUS_ROLLED_BACK) {
                        try {
                            fileStorage.deleteByKey(finalUploadKey);
                        } catch (Exception ex) {
                            log.error("S3 濡ㅻ갚 ?뚯씪 ??젣 ?ㅽ뙣 - key: {}", finalUploadKey, ex);
                        }
                    }
                }
            });

            freelancer.updateBasicProfile(null, uploadedUrl, null);

            return uploadedUrl;
        } catch (BusinessException e) {
            throw e;
        } catch (IOException e) {
            log.error("S3 ??낆쨮????쎈솭 - userId: {}, fileName: {}", userId, file.getOriginalFilename(), e);
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR);
        } catch (RuntimeException e) {
            log.error("S3 ??낆쨮????쎈솭 - userId: {}, key: {}", userId, uploadKey, e);
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    private Freelancer findByUserIdOrThrow(Long userId) {
        return freelancerRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
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

    private Double calculateTotalScore(Expertise expertise, Collaboration collaboration, Double fallbackAverageRate) {
        int count = 0;
        double total = 0.0;

        if (expertise != null) {
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
        }

        if (collaboration != null) {
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
        }

        if (count == 0) {
            return fallbackAverageRate;
        }

        return total / count;
    }
}

