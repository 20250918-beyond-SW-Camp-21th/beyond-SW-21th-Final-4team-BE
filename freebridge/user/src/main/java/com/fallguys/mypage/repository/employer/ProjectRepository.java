package com.fallguys.mypage.repository.employer;

import org.springframework.stereotype.Repository;

/**
 * 프로젝트 도메인과의 통신을 위한 임시 레포지토리 인터페이스.
 * 추후 FeignClient 나 모듈 간 직접 참조 방식으로 변경될 수 있습니다.
 */
@Repository
public interface ProjectRepository {
    int countCompletedProjectsByEmployerId(Long employerId);
}
