package com.fallguys.user.api.shared;

import com.fallguys.user.api.shared.dto.ExternalUserResponse;

/**
 * 타 모듈에서 User 정보를 조회하기 위해 사용할 인터페이스입니다.
 * 이 인터페이스를 통해 user 모듈 내부의 UserService와 강한 결합 없이 안전한 통신이 가능합니다.
 */
public interface ExternalUserApi {

    /**
     * 사용자 ID로 유저 정보를 조회합니다.
     * 
     * @param userId 조회할 사용자의 고유 ID
     * @return 유저 정보 DTO (존재하지 않으면 예외 발생)
     */
    ExternalUserResponse getUserById(Long userId);

    /**
     * 이메일로 유저 정보를 조회합니다.
     * 
     * @param email 조회할 사용자의 이메일 주소
     * @return 유저 정보 DTO (존재하지 않으면 예외 발생)
     */
    ExternalUserResponse getUserByEmail(String email);

    /**
     * 사용자 ID 존재 여부를 확인합니다.
     * 
     * @param userId 확인할 사용자 고유 ID
     * @return 존재할 경우 true, 그렇지 않으면 false
     */
    boolean existsById(Long userId);
}
