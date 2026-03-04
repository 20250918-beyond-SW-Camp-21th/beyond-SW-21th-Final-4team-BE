package com.fallguys.userlike.api.web;

import com.fallguys.common.response.ApiResponse;
import com.fallguys.userlike.api.support.UserLikeTokenUserIdResolver;
import com.fallguys.userlike.service.EmployerFreelancerFavoriteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Employer MyPage - Favorite", description = "고용주 즐겨찾기(프리랜서) 관리 API")
@RestController
@RequiredArgsConstructor
public class EmployerFreelancerFavoriteController {

    private final EmployerFreelancerFavoriteService favoriteService;
    private final UserLikeTokenUserIdResolver tokenUserIdResolver;

    @Operation(summary = "프리랜서 즐겨찾기 등록", description = "고용주가 프리랜서를 즐겨찾기에 등록합니다.")
    @PostMapping("/api/employer/freelancers/{freelancerId}/like")
    public ResponseEntity<ApiResponse<Void>> addFavorite(
            @RequestHeader("Authorization") String authorization,
            @PathVariable Long freelancerId
    ) {
        Long employerId = tokenUserIdResolver.resolveUserId(authorization);
        favoriteService.addFavorite(employerId, freelancerId);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @Operation(summary = "프리랜서 즐겨찾기 삭제", description = "고용주가 프리랜서 즐겨찾기를 해제합니다.")
    @DeleteMapping("/api/employer/freelancers/{freelancerId}/like")
    public ResponseEntity<ApiResponse<Void>> removeFavorite(
            @RequestHeader("Authorization") String authorization,
            @PathVariable Long freelancerId
    ) {
        Long employerId = tokenUserIdResolver.resolveUserId(authorization);
        favoriteService.removeFavorite(employerId, freelancerId);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}
