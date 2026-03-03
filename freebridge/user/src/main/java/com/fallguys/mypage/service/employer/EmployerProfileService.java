package com.fallguys.mypage.service.employer;

import com.fallguys.common.port.FileStorage;
import com.fallguys.mypage.api.web.dto.employer.response.EmployerBasicProfileDto;
import com.fallguys.mypage.api.web.dto.employer.request.EmployerProfileUpdateRequestDto;
import com.fallguys.mypage.entity.employer.Employer;
import com.fallguys.mypage.repository.employer.EmployerRepository;
import com.fallguys.mypage.api.web.dto.employer.response.CrmAlertsResponseDto;
import com.fallguys.mypage.entity.employer.Subscription;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.UUID;
import java.util.Set;
import com.fallguys.mypage.entity.employer.Scale;


@Service
@RequiredArgsConstructor
public class EmployerProfileService {

    private final EmployerRepository employerRepository;
    private final FileStorage fileStorage;

    @Transactional(readOnly = true)
    public EmployerBasicProfileDto getProfile(Long userId) {
        Employer employer = employerRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("해당 유저의 고용주 프로필을 찾을 수 없습니다."));

        return EmployerBasicProfileDto.from(employer);
    }

    @Transactional
    public void updateProfile(Long userId, EmployerProfileUpdateRequestDto request) {
        Employer employer = employerRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("해당 유저의 고용주 프로필을 찾을 수 없습니다."));

        employer.updateProfile(
                request.companyName(),
                request.industry(),
                parseScale(request.scale()),
                request.location(),
                request.websiteUrl(),
                request.description(),
                employer.getLogoUrl() // 기존 로고는 유지 (로고 수정 API 분리됨)
        );
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

    @Transactional
    public String updateLogoUrl(Long userId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("업로드할 파일이 없습니다.");
        }

        String contentType = file.getContentType();
        Set<String> allowedMimeTypes = Set.of("image/jpeg", "image/png", "image/webp", "image/gif");
        
        if (contentType == null || !allowedMimeTypes.contains(contentType.toLowerCase())) {
            throw new IllegalArgumentException("이미지 파일(JPEG, PNG, WEBP, GIF)만 업로드 가능합니다. (현재 타입: " + contentType + ")");
        }

        Employer employer = employerRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("해당 유저의 고용주 프로필을 찾을 수 없습니다."));

        try {
            // S3 키(경로) 생성 (예: employers/logo/uuid_filename)
            String extension = getExtension(file.getOriginalFilename());
            String key = "employers/logo/" + UUID.randomUUID() + extension;
            
            // S3 FileStorage 인터페이스를 통한 업로드 실제 수행
            // 반환되는 key는 S3에 저장된 경로
            String uploadedUrl = fileStorage.upload(file.getBytes(), key, file.getContentType());
            
            // DB 엔티티 업데이트
            employer.updateLogoUrl(uploadedUrl);
            
            return uploadedUrl;
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

    @Transactional(readOnly = true)
    public com.fallguys.mypage.api.web.dto.employer.response.CrmAlertsResponseDto getCrmAlerts(Long userId) {
        Employer employer = employerRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("해당 유저의 고용주 프로필을 찾을 수 없습니다."));
        
        // BASIC 요금제인 경우 업셀링 대상으로 간주
        boolean isPremiumUpsellEligible = (employer.getSubscription() == Subscription.BASIC);

        return new CrmAlertsResponseDto(isPremiumUpsellEligible);
    }
}
