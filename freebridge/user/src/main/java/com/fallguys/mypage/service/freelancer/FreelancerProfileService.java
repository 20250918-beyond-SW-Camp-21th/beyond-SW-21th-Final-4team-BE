package com.fallguys.mypage.service.freelancer;


import com.fallguys.common.port.FileStorage;
import com.fallguys.mypage.api.web.dto.freelancer.request.FreelancerProfileUpdateRequestDto;
import com.fallguys.mypage.api.web.dto.freelancer.response.FreelancerBasicProfileDto;
import com.fallguys.mypage.api.web.dto.freelancer.response.FreelancerProfileResponseDto;
import com.fallguys.mypage.api.web.dto.freelancer.response.FreelancerStatsDto;
import com.fallguys.mypage.entity.freelancer.Freelancer;
import com.fallguys.mypage.repository.freelancer.FreelancerRepository;
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

    private static final long MAX_AVATAR_BYTES = 5 * 1024 * 1024; // 5MB

    @Transactional(readOnly = true)
    public FreelancerProfileResponseDto getProfile(Long userId) {
        Freelancer freelancer = findByUserIdOrThrow(userId);

        FreelancerBasicProfileDto basicProfile = new FreelancerBasicProfileDto(
                freelancer.getAvatarUrl(),
                null, // name 은 User 도메인 정보 → 현재 미연동, 추후 ExternalUserApi 확장 예정
                freelancer.getJob(),
                freelancer.getIntroduction(),
                freelancer.getGrade() != null ? freelancer.getGrade().name() : null,
                freelancer.getCareerYears(),
                freelancer.getWage(),
                freelancer.getSkills(),
                freelancer.getStatus() != null ? freelancer.getStatus().name() : null
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
    }

    @Transactional
    public String updateAvatarUrl(Long userId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("업로드할 파일이 없습니다.");
        }

        if (file.getSize() > MAX_AVATAR_BYTES) {
            throw new IllegalArgumentException("업로드 파일 크기는 5MB를 초과할 수 없습니다.");
        }

        String contentType = file.getContentType();
        Set<String> allowedTypes = Set.of("image/jpeg", "image/png", "image/webp", "image/gif");
        if (contentType == null || !allowedTypes.contains(contentType.toLowerCase())) {
            throw new IllegalArgumentException("이미지 파일(JPEG, PNG, WEBP, GIF)만 업로드 가능합니다. (현재 타입: " + contentType + ")");
        }

        try {
            byte[] fileBytes = file.getBytes();
            if (!isValidImageByMagicBytes(fileBytes)) {
                throw new IllegalArgumentException("올바른 이미지 파일 형식이 아닙니다. (확장자 위조 의심)");
            }

            Freelancer freelancer = findByUserIdOrThrow(userId);

            String extension = getExtension(file.getOriginalFilename());
            String key = "freelancers/avatar/" + UUID.randomUUID() + extension;
            String uploadedUrl = fileStorage.upload(fileBytes, key, file.getContentType());

            // DB 트랜잭션 롤백 시 업로드된 S3 파일 삭제 (고아 파일 방지)
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCompletion(int status) {
                    if (status == STATUS_ROLLED_BACK) {
                        try {
                            fileStorage.deleteByKey(key);
                        } catch (Exception ex) {
                            log.error("S3 롤백 삭제 실패 - key: {}", key, ex);
                        }
                    }
                }
            });

            freelancer.updateBasicProfile(null, uploadedUrl, null);

            return uploadedUrl;
        } catch (IOException e) {
            throw new RuntimeException("파일 업로드 중 오류가 발생했습니다.", e);
        }
    }

    private Freelancer findByUserIdOrThrow(Long userId) {
        return freelancerRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("해당 유저의 프리랜서 프로필을 찾을 수 없습니다."));
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
}
