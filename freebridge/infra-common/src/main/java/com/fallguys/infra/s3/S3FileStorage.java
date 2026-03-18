package com.fallguys.infra.s3;

import com.fallguys.common.port.FileStorage;
import com.fallguys.infra.s3.support.S3Properties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

@Component
@RequiredArgsConstructor
public class S3FileStorage implements FileStorage {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final S3Properties properties;

    @Override
    public String upload(byte[] bytes, String key, String contentType) {
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(properties.getBucket())
                .key(key)
                .contentType(contentType)
                .build();

        s3Client.putObject(putObjectRequest, RequestBody.fromBytes(bytes));

        return key;
    }

    @Override
    public void deleteByKey(String key) {
        DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                .bucket(properties.getBucket())
                .key(key)
                .build();

        s3Client.deleteObject(deleteObjectRequest);
    }

    @Override
    public String generatePresignedUrl(String key) {
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(properties.getBucket())
                .key(key)
                .build();

        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(properties.getPresignedUrlExpirationMinutes()))
                .getObjectRequest(getObjectRequest)
                .build();

        return s3Presigner.presignGetObject(presignRequest).url().toString();
    }

    @Override
    public String generatePresignedDownloadUrl(String key, String fileName) {
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(properties.getBucket())
                .key(key)
                .responseContentDisposition(buildAttachmentContentDisposition(fileName))
                .build();

        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(properties.getPresignedUrlExpirationMinutes()))
                .getObjectRequest(getObjectRequest)
                .build();

        return s3Presigner.presignGetObject(presignRequest).url().toString();
    }

    private String buildAttachmentContentDisposition(String fileName) {
        String sanitizedFileName = fileName == null ? "" : fileName.replaceAll("\\p{Cntrl}", "");
        String normalized = sanitizedFileName.isBlank() ? "attachment" : sanitizedFileName.trim();
        String asciiFallback = normalized
                .replaceAll("[^\\x20-\\x7E]", "_")
                .replace("\\", "_")
                .replace("\"", "_")
                .replace(";", "_");
        if (asciiFallback.isBlank()) {
            asciiFallback = "attachment";
        }
        String encodedFileName = URLEncoder.encode(normalized, StandardCharsets.UTF_8)
                .replace("+", "%20");

        return "attachment; filename=\"" + asciiFallback + "\"; filename*=UTF-8''" + encodedFileName;
    }
}
