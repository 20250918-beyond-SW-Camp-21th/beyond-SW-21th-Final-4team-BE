package com.fallguys.mypage.api.web.freelancer;

import com.fallguys.common.exception.BusinessException;
import com.fallguys.common.exception.ErrorCode;
import com.fallguys.common.response.ApiResponse;
import com.fallguys.common.security.CustomUserDetails;
import com.fallguys.mypage.api.web.dto.freelancer.response.PortfolioInfoDto;
import com.fallguys.mypage.service.freelancer.FreelancerPortfolioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.net.URLEncoder;

@Tag(name = "6. Freelancer MyPage - Portfolio", description = "프리랜서 마이페이지 포트폴리오 API")
@RestController
@RequestMapping("/api/freelancer/mypage/portfolio")
@RequiredArgsConstructor
public class FreelancerPortfolioController {

    private final FreelancerPortfolioService freelancerPortfolioService;

    @Operation(summary = "포트폴리오 조회", description = "프리랜서 포트폴리오 파일 정보를 조회합니다.")
    @GetMapping
    public ApiResponse<PortfolioInfoDto> getPortfolio(@AuthenticationPrincipal CustomUserDetails userDetails) {
        if (userDetails == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return ApiResponse.ok(freelancerPortfolioService.getPortfolio(userDetails.getId()));
    }

    @Operation(summary = "포트폴리오 업로드", description = "프리랜서 포트폴리오 파일을 업로드합니다.")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<PortfolioInfoDto> uploadPortfolio(@AuthenticationPrincipal CustomUserDetails userDetails,
                                                         @RequestPart("file") MultipartFile file) {
        if (userDetails == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return ApiResponse.ok(freelancerPortfolioService.uploadPortfolio(userDetails.getId(), file));
    }

    @GetMapping("/download")
    public ApiResponse<String> getPortfolioDownloadUrl(@AuthenticationPrincipal CustomUserDetails userDetails) {
        if (userDetails == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return ApiResponse.ok(freelancerPortfolioService.getPortfolioDownloadUrl(userDetails.getId()));
    }

    @DeleteMapping
    public ApiResponse<Void> deletePortfolio(@AuthenticationPrincipal CustomUserDetails userDetails) {
        if (userDetails == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        freelancerPortfolioService.deletePortfolio(userDetails.getId());
        return ApiResponse.ok(null);
    }

    @GetMapping("/template")
    public ResponseEntity<byte[]> downloadTemplate(@AuthenticationPrincipal CustomUserDetails userDetails) throws IOException {
        if (userDetails == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        TemplateResource templateResource = resolveTemplateResource();
        ClassPathResource resource = new ClassPathResource(templateResource.path());
        byte[] bytes;
        try (InputStream in = resource.getInputStream()) {
            bytes = in.readAllBytes();
        }

        return ResponseEntity.ok()
                .contentType(templateResource.mediaType())
                .header(HttpHeaders.CONTENT_DISPOSITION, buildAttachmentHeader(templateResource.fileName()))
                .body(bytes);
    }

    private TemplateResource resolveTemplateResource() {
        ClassPathResource docx = new ClassPathResource("templates/freelancer-portfolio-template.docx");
        if (docx.exists()) {
            return new TemplateResource(
                    "templates/freelancer-portfolio-template.docx",
                    "freelancer-portfolio-template.docx",
                    MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.wordprocessingml.document")
            );
        }

        ClassPathResource pdf = new ClassPathResource("templates/freelancer-portfolio-template.pdf");
        if (pdf.exists()) {
            return new TemplateResource(
                    "templates/freelancer-portfolio-template.pdf",
                    "freelancer-portfolio-template.pdf",
                    MediaType.APPLICATION_PDF
            );
        }

        throw new IllegalStateException(
                "Portfolio template not found: templates/freelancer-portfolio-template.docx or templates/freelancer-portfolio-template.pdf"
        );
    }

    private record TemplateResource(
            String path,
            String fileName,
            MediaType mediaType
    ) {
    }

    private String buildAttachmentHeader(String fileName) {
        String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8)
                .replace("+", "%20");
        return "attachment; filename=\"" + fileName + "\"; filename*=UTF-8''" + encodedFileName;
    }
}
