package com.fallguys.mypage.service.freelancer;

import com.fallguys.common.exception.BusinessException;
import com.fallguys.common.exception.ErrorCode;
import com.fallguys.common.port.FileStorage;
import com.fallguys.mypage.api.web.dto.freelancer.response.PortfolioInfoDto;
import com.fallguys.mypage.entity.freelancer.Freelancer;
import com.fallguys.mypage.entity.freelancer.PortfolioInfo;
import com.fallguys.mypage.repository.freelancer.FreelancerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class FreelancerPortfolioService {

    private static final long MAX_PORTFOLIO_BYTES = 20 * 1024 * 1024; // 20MB
    private static final Set<String> ALLOWED_TYPES = Set.of(
            "application/pdf",
            "application/x-pdf",
            "application/octet-stream"
    );

    private final FreelancerRepository freelancerRepository;
    private final FileStorage fileStorage;

    @Transactional(readOnly = true)
    public PortfolioInfoDto getPortfolio(Long userId) {
        Freelancer freelancer = findByUserIdOrThrow(userId);
        PortfolioInfo info = freelancer.getPortfolioInfo();
        if (info == null) {
            return new PortfolioInfoDto(null, null, null);
        }
        return new PortfolioInfoDto(toAccessibleUrl(info.getPortfolioFileUrl()), info.getPortfolioFileName(), info.getPortfolioLastUpdated());
    }

    @Transactional
    public PortfolioInfoDto uploadPortfolio(Long userId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }

        if (file.getSize() > MAX_PORTFOLIO_BYTES) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }

        String contentType = file.getContentType();
        String extension = getExtension(file.getOriginalFilename()).toLowerCase();
        if (contentType == null || !ALLOWED_TYPES.contains(contentType.toLowerCase()) || !".pdf".equals(extension)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }

        String uploadKey = null;
        try {
            byte[] fileBytes = file.getBytes();
            if (!isValidPdfByMagicBytes(fileBytes)) {
                throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
            }

            Freelancer freelancer = findByUserIdOrThrow(userId);
            uploadKey = "freelancers/portfolio/" + UUID.randomUUID() + extension;
            String uploadedKey = fileStorage.upload(fileBytes, uploadKey, contentType);
            String previousKey = freelancer.getPortfolioInfo() != null
                    ? freelancer.getPortfolioInfo().getPortfolioFileUrl()
                    : null;

            final String finalUploadedKey = uploadedKey;
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCompletion(int status) {
                    if (status == STATUS_ROLLED_BACK) {
                        try {
                            fileStorage.deleteByKey(finalUploadedKey);
                        } catch (Exception ex) {
                            log.error("S3 rollback delete failed. key: {}", finalUploadedKey, ex);
                        }
                    }
                }
            });

            PortfolioInfo info = new PortfolioInfo(uploadedKey, file.getOriginalFilename(), LocalDateTime.now());
            freelancer.updatePortfolioInfo(info);

            if (isStoredKey(previousKey) && !previousKey.equals(uploadedKey)) {
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        try {
                            fileStorage.deleteByKey(previousKey);
                        } catch (Exception ex) {
                            log.error("S3 old portfolio delete failed. key: {}", previousKey, ex);
                        }
                    }
                });
            }

            return new PortfolioInfoDto(toAccessibleUrl(info.getPortfolioFileUrl()), info.getPortfolioFileName(), info.getPortfolioLastUpdated());
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

    @Transactional(readOnly = true)
    public String getPortfolioDownloadUrl(Long userId) {
        Freelancer freelancer = findByUserIdOrThrow(userId);
        PortfolioInfo info = freelancer.getPortfolioInfo();
        if (info == null || info.getPortfolioFileUrl() == null || info.getPortfolioFileUrl().isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
        return toAccessibleUrl(info.getPortfolioFileUrl());
    }

    @Transactional
    public void deletePortfolio(Long userId) {
        Freelancer freelancer = findByUserIdOrThrow(userId);
        PortfolioInfo info = freelancer.getPortfolioInfo();
        if (info == null || info.getPortfolioFileUrl() == null || info.getPortfolioFileUrl().isBlank()) {
            return;
        }

        String previousKey = info.getPortfolioFileUrl();
        freelancer.clearPortfolioInfo();

        if (isStoredKey(previousKey)) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    try {
                        fileStorage.deleteByKey(previousKey);
                    } catch (Exception ex) {
                        log.error("S3 portfolio delete failed. key: {}", previousKey, ex);
                    }
                }
            });
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

    private boolean isValidPdfByMagicBytes(byte[] bytes) {
        if (bytes == null || bytes.length < 4) {
            return false;
        }
        return bytes[0] == '%' && bytes[1] == 'P' && bytes[2] == 'D' && bytes[3] == 'F';
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
}
