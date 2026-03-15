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
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

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
}
